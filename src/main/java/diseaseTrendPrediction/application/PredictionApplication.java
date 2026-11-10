package diseaseTrendPrediction.application;

import com.google.common.base.Preconditions;
import diseaseTrendPrediction.application.exceptions.SourceTimestampReadException;
import diseaseTrendPrediction.domain.MarkovModel;
import diseaseTrendPrediction.domain.PatientHistory;
import diseaseTrendPrediction.domain.PredictionResult;
import diseaseTrendPrediction.io.CacheCheck;
import diseaseTrendPrediction.io.exceptions.PatientCsvReadException;
import diseaseTrendPrediction.io.exceptions.TransitionDataIntegrityException;
import diseaseTrendPrediction.io.exceptions.TransitionDataNotFoundException;
import diseaseTrendPrediction.io.exceptions.TransitionDataPersistenceException;
import diseaseTrendPrediction.io.exceptions.TransitionDataReadException;
import diseaseTrendPrediction.io.patientCSV.PatientCsvReader;
import diseaseTrendPrediction.io.transitionDataStorage.MatrixStore;
import diseaseTrendPrediction.io.transitionDataStorage.TransitionData;
import diseaseTrendPrediction.io.exceptions.TransitionDataPersistenceException;
import diseaseTrendPrediction.logic.PredictionLogic;
import diseaseTrendPrediction.logic.TransitionMatrixBuilder;
import diseaseTrendPrediction.logic.exceptions.EmptyPatientHistoriesException;
import diseaseTrendPrediction.logic.exceptions.NegativePredictionStepsException;
import diseaseTrendPrediction.logic.exceptions.PredictionMatrixDimensionMismatchException;
import diseaseTrendPrediction.logic.exceptions.UnstablePredictionDistributionException;
import linearAlgebra.domain.Exceptions.NotDiagonalizableException;
import linearAlgebra.domain.Diagonalization;
import linearAlgebra.domain.Vector;
import linearAlgebra.logic.DiagonalizationLogic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Coordinates model preparation, cache use, and predictions for the user interface. */
public final class PredictionApplication {

    private final MatrixStore matrixStore;
    private final Map<String, Double> epsilonProfiles;
    private final String defaultEpsilonProfile;

    /**
     * Creates the application coordinator.
     *
     * @param matrixStore persistence for the active model snapshot
     * @param epsilonProfiles available tolerance profiles loaded during startup
     * @param defaultEpsilonProfile default tolerance profile name
     */
    public PredictionApplication(
            final MatrixStore matrixStore,
            final Map<String, Double> epsilonProfiles,
            final String defaultEpsilonProfile
    ) {
        Preconditions.checkNotNull(
            matrixStore,
            "Matrix store must not be null"
        );
        Preconditions.checkNotNull(
            epsilonProfiles,
            "Epsilon profiles must not be null"
        );
        Preconditions.checkNotNull(
            defaultEpsilonProfile,
            "Default epsilon profile must not be null"
        );
        Preconditions.checkArgument(
            !epsilonProfiles.isEmpty(),
            "At least one epsilon profile is required"
        );

        for (Map.Entry<String, Double> profile : epsilonProfiles.entrySet()) {
            Preconditions.checkNotNull(
                profile.getKey(),
                "Epsilon profile name must not be null"
            );
            Preconditions.checkArgument(
                !profile.getKey().isBlank(),
                "Epsilon profile name must not be blank"
            );
            Preconditions.checkNotNull(
                profile.getValue(),
                "Epsilon profile value must not be null"
            );
            Preconditions.checkArgument(
                Double.isFinite(profile.getValue()) &&
                profile.getValue() >= 0,
                "Epsilon profile values must be finite and non-negative"
            );
        }

        this.matrixStore = matrixStore;
        this.epsilonProfiles = Map.copyOf(epsilonProfiles);
        this.defaultEpsilonProfile = defaultEpsilonProfile;
        checkPredictionApplication();
    }

    /**
     * Returns the immutable epsilon profiles available to the interface.
     *
     * @return profile names and tolerance values
     */
    public Map<String, Double> epsilonProfiles() {
        checkPredictionApplication();
        return epsilonProfiles;
    }

    /**
     * Returns the default tolerance profile name.
     *
     * @return default profile name
     */
    public String defaultEpsilonProfile() {
        checkPredictionApplication();
        return defaultEpsilonProfile;
    }

    /**
     * Returns whether a complete cached model snapshot is available.
     *
     * @return whether a snapshot is present
     * @throws TransitionDataIntegrityException if the snapshot is invalid
     * @throws TransitionDataReadException if the snapshot cannot be read
     */
    public boolean hasCachedSnapshot()
            throws TransitionDataIntegrityException,
            TransitionDataReadException
    {
        checkPredictionApplication();
        boolean snapshotAvailable = true;
        try {
            matrixStore.loadTransitionData();
        } catch (TransitionDataNotFoundException e) {
            snapshotAvailable = false;
        }
        return snapshotAvailable;
    }

    /**
     * Returns whether the cached model matches the source file timestamp.
     *
     * @param sourceCsv patient source file
     * @return whether a snapshot exists and matches the source timestamp
     * @throws SourceTimestampReadException if the source timestamp cannot be read
     * @throws TransitionDataIntegrityException if the snapshot is invalid
     * @throws TransitionDataReadException if the cached snapshot cannot be read
     */
    public boolean isCacheCurrent(
            final Path sourceCsv
    )
            throws SourceTimestampReadException,
            TransitionDataIntegrityException,
            TransitionDataReadException
    {
        checkPredictionApplication();
        Preconditions.checkNotNull(
            sourceCsv,
            "Source CSV must not be null"
        );

        boolean cacheCurrent = false;
        try {
            final TransitionData cached = matrixStore.loadTransitionData();
            final Instant sourceTimestamp;
            try {
                sourceTimestamp = Files.getLastModifiedTime(sourceCsv).toInstant();
            } catch (IOException e) {
                throw new SourceTimestampReadException();
            }
            cacheCurrent = CacheCheck.isUpToDate(sourceTimestamp, cached.sourceTimestamp());
        } catch (TransitionDataNotFoundException e) {
        }
        return cacheCurrent;
    }

    /**
     * Prepares prediction logic from either the cached model or a CSV rebuild.
     *
     * @param sourceCsv CSV used when rebuilding the model
     * @param epsilonProfile selected tolerance profile name
     * @param decision whether to use the snapshot or rebuild it
     * @return prediction logic configured with the selected tolerance
     * @throws PatientCsvReadException if the CSV cannot be read
     * @throws SourceTimestampReadException if the source timestamp cannot be obtained
     * @throws TransitionDataNotFoundException if the selected cached snapshot is absent
     * @throws TransitionDataIntegrityException if the cached snapshot is invalid
     * @throws TransitionDataReadException if the cached snapshot cannot be loaded
     * @throws TransitionDataPersistenceException if the rebuilt snapshot cannot be saved
     * @throws EmptyPatientHistoriesException if no transitions can be built
     * @throws NotDiagonalizableException if the rebuilt model cannot be diagonalized
     */
    public PredictionLogic prepareModel(
            final Path sourceCsv,
            final String epsilonProfile,
            final CacheDecision decision
    )
            throws PatientCsvReadException,
            SourceTimestampReadException,
            TransitionDataNotFoundException,
            TransitionDataIntegrityException,
            TransitionDataReadException,
            TransitionDataPersistenceException,
            EmptyPatientHistoriesException,
            NotDiagonalizableException
    {
        checkPredictionApplication();
        Preconditions.checkNotNull(
            sourceCsv,
            "Source CSV must not be null"
        );
        Preconditions.checkNotNull(
            epsilonProfile,
            "Epsilon profile must not be null"
        );
        Preconditions.checkNotNull(
            decision,
            "Cache decision must not be null"
        );

        final Double epsilon = epsilonProfiles.get(epsilonProfile);
        Preconditions.checkArgument(
            epsilon != null,
            "Epsilon profile must be available"
        );

        final PredictionLogic predictionLogic;
        if (decision == CacheDecision.USE_CACHED) {
            final TransitionData cached = matrixStore.loadTransitionData();
            predictionLogic = new PredictionLogic(cached.diagonalization(), epsilon);
        } else {
            predictionLogic = rebuildModel(sourceCsv, epsilon);
        }
        return predictionLogic;
    }

    /**
     * Predicts the distribution after the requested number of steps.
     *
     * @param predictionLogic prepared prediction logic
     * @param initialDistribution starting health-state distribution
     * @param steps number of transitions to apply
     * @return prediction result
     * @throws NegativePredictionStepsException if steps is negative
     * @throws PredictionMatrixDimensionMismatchException if matrices cannot be multiplied
     * @throws UnstablePredictionDistributionException if numerical correction exceeds epsilon
     */
    public PredictionResult predict(
            final PredictionLogic predictionLogic,
            final Vector initialDistribution,
            final int steps
    ) {
        checkPredictionApplication();
        Preconditions.checkNotNull(
            predictionLogic,
            "Prediction logic must not be null"
        );
        Preconditions.checkNotNull(
            initialDistribution,
            "Initial distribution must not be null"
        );
        return predictionLogic.predict(initialDistribution, steps);
    }

    // Builds and persists a model from patient histories.
    private PredictionLogic rebuildModel(
            final Path sourceCsv,
            final double epsilon
    )
            throws PatientCsvReadException,
            SourceTimestampReadException,
            TransitionDataPersistenceException,
            EmptyPatientHistoriesException,
            NotDiagonalizableException
    {
        final Instant sourceTimestamp;
        try {
            sourceTimestamp = Files.getLastModifiedTime(sourceCsv).toInstant();
        } catch (IOException e) {
            throw new SourceTimestampReadException();
        }
        final List<PatientHistory> histories;
        histories = PatientCsvReader.read(sourceCsv);

        final MarkovModel model = TransitionMatrixBuilder.build(histories, epsilon);
        final Diagonalization diagonalization =
                new DiagonalizationLogic(model.model()).diagonalize();

        matrixStore.saveTransitionData(
                model.model(),
                diagonalization,
                sourceTimestamp
        );
        return new PredictionLogic(diagonalization, epsilon);
    }

    // Checks that this coordinator has its required dependencies.
    private void checkPredictionApplication() {
        Preconditions.checkNotNull(
            matrixStore,
            "Matrix store must be initialized"
        );
        Preconditions.checkNotNull(
            epsilonProfiles,
            "Epsilon profiles must be initialized"
        );
        Preconditions.checkState(
            !epsilonProfiles.isEmpty() &&
            epsilonProfiles.containsKey(defaultEpsilonProfile),
            "Epsilon profiles must include the default profile"
        );
    }
}
