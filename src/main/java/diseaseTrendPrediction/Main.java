package diseaseTrendPrediction;

import diseaseTrendPrediction.application.PredictionApplication;
import diseaseTrendPrediction.io.EpsilonProfileStore;
import diseaseTrendPrediction.io.exceptions.DatabaseDirectoryCreationException;
import diseaseTrendPrediction.io.exceptions.DatabaseInitializationException;
import diseaseTrendPrediction.io.exceptions.EpsilonProfileNotFoundException;
import diseaseTrendPrediction.io.exceptions.EpsilonProfileReadException;
import diseaseTrendPrediction.io.exceptions.UnsupportedDatabaseSchemaException;
import diseaseTrendPrediction.io.transitionDataStorage.Database;
import diseaseTrendPrediction.io.transitionDataStorage.MatrixStore;

import java.nio.file.Path;
import java.util.Map;

/** Composes the dependencies used by the disease trend prediction application. */
public final class Main {

    private static final String DEFAULT_EPSILON_PROFILE = "strict";
    private static final String MEDIOCRE_EPSILON_PROFILE = "mediocre";
    private static final String LOOSE_EPSILON_PROFILE = "loose";

    // Prevents instances of the application entry point.
    private Main() {}

    /**
     * Initializes application dependencies.
     *
     * @param args command-line arguments
     */
    public static void main(
            final String[] args
    ) {
        try {
            final PredictionApplication application = predictDiseaseTrends();
            System.out.println(
                    "Application dependencies initialized with default epsilon profile: " +
                    application.defaultEpsilonProfile()
            );
        } catch (
            DatabaseDirectoryCreationException |
            DatabaseInitializationException |
            UnsupportedDatabaseSchemaException |
            EpsilonProfileNotFoundException |
            EpsilonProfileReadException e
        ) {
            System.err.println(
                    "Unable to start disease trend prediction: " +
                    e.getClass().getSimpleName()
            );
            System.exit(1);
        }
    }

    // Initializes persistence and composes the application coordinator.
    private static PredictionApplication predictDiseaseTrends()
            throws DatabaseDirectoryCreationException,
            DatabaseInitializationException,
            UnsupportedDatabaseSchemaException,
            EpsilonProfileNotFoundException,
            EpsilonProfileReadException
    {
        final Path dataDirectory = Path.of("resources", "data");
        final Path databasePath = dataDirectory.resolve("transition-cache.sqlite");

        Database.initialize(databasePath);

        final EpsilonProfileStore profileStore = new EpsilonProfileStore(databasePath);
        final Map<String, Double> epsilonProfiles = Map.of(
                DEFAULT_EPSILON_PROFILE,
                profileStore.load(DEFAULT_EPSILON_PROFILE),
                MEDIOCRE_EPSILON_PROFILE,
                profileStore.load(MEDIOCRE_EPSILON_PROFILE),
                LOOSE_EPSILON_PROFILE,
                profileStore.load(LOOSE_EPSILON_PROFILE)
        );

        return new PredictionApplication(
                new MatrixStore(databasePath),
                epsilonProfiles,
                DEFAULT_EPSILON_PROFILE
        );
    }
}
