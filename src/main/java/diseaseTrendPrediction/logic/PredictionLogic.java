package diseaseTrendPrediction.logic;

import linearAlgebra.domain.Diagonalization;
import linearAlgebra.domain.Exceptions.MatrixMultiplyDimMismatchException;
import linearAlgebra.domain.Matrix;
import linearAlgebra.domain.Vector;

import com.google.common.base.Preconditions;

import diseaseTrendPrediction.domain.HealthState;
import diseaseTrendPrediction.domain.PredictionResult;
import diseaseTrendPrediction.domain.StateVector;
import diseaseTrendPrediction.logic.exceptions.InvalidPredictionDiagonalizationSizeException;
import diseaseTrendPrediction.logic.exceptions.NegativePredictionStepsException;
import diseaseTrendPrediction.logic.exceptions.PredictionMatrixDimensionMismatchException;
import diseaseTrendPrediction.logic.exceptions.UnstablePredictionDistributionException;
import linearAlgebra.logic.MatrixLogic;

/** Predicts future health-state distributions from a transition model. */
public final class PredictionLogic {

    private final Diagonalization diagonalization;
    private final double epsilon;
    // Holds matrix logic for the diagonalization basis.
    private final MatrixLogic pLogic;

    /**
     * Initializes prediction logic for a health-state transition model.
     *
     * @param diagonalization transition matrix diagonalization
     * @param epsilon tolerance for distribution validation and numerical correction
     */
    public PredictionLogic(
            final Diagonalization diagonalization,
            final double epsilon
    ) {
        Preconditions.checkNotNull(
            diagonalization,
            "Diagonalization must not be null"
        );
        Preconditions.checkArgument(
            Double.isFinite(epsilon) &&
            epsilon >= 0,
            "Epsilon must be finite and non-negative"
        );
        if (diagonalization.P().rows() != HealthState.values().length) {
            throw new InvalidPredictionDiagonalizationSizeException();
        }

        this.diagonalization = diagonalization;
        this.epsilon = epsilon;
        this.pLogic = new MatrixLogic(diagonalization.P());
        checkPredictionLogic();
    }

    /**
     * Predicts a health-state distribution after a number of steps.
     *
     * @param initial starting distribution
     * @param steps number of transitions to apply
     * @return prediction result
     * @throws NegativePredictionStepsException if steps is negative
     * @throws PredictionMatrixDimensionMismatchException if matrices cannot be multiplied
     * @throws UnstablePredictionDistributionException if numerical correction exceeds epsilon
     */
    public PredictionResult predict(
            final Vector initial,
            final int steps
    )
            throws NegativePredictionStepsException,
            PredictionMatrixDimensionMismatchException,
            UnstablePredictionDistributionException
    {
        Preconditions.checkNotNull(
            initial,
            "Initial distribution must not be null"
        );
        if (steps < 0) {
            throw new NegativePredictionStepsException();
        }

        final StateVector start = new StateVector(initial, epsilon);
        final PredictionResult result;
        if (steps == 0) {
            result = new PredictionResult(0, start, start, start.mostLikelyStates());
        } else {
            final Vector computed = getTransitionPower(steps).multiply(start.vector());
            final StateVector prediction = new StateVector(getWellDefined(computed), epsilon);
            result = new PredictionResult(
                    steps,
                    start,
                    prediction,
                    prediction.mostLikelyStates()
            );
        }
        return result;
    }
    // Computes a transition matrix power for the requested number of steps.
    private Matrix getTransitionPower(
            final int steps
    ) {
        final Matrix eigenvalues = diagonalization.D();
        final int size = eigenvalues.rows();
        final double[][] poweredData = new double[size][size];
        for (int i = 0; i < size; i++) {
            poweredData[i][i] = Math.pow(eigenvalues.get(i, i), steps);
        }
        try {
            final Matrix scaledInverse = new MatrixLogic(new Matrix(poweredData))
                    .multiply(diagonalization.inverseP());
            return pLogic.multiply(scaledInverse);

        } catch (MatrixMultiplyDimMismatchException e) {
            throw new PredictionMatrixDimensionMismatchException();
        }
    }
    // Validates and corrects a computed probability distribution.
    private Vector getWellDefined(
            final Vector computed
    ) {
        Vector wellDefined = computed;
        if (!StateVector.isDistribution(computed, epsilon)) {
            double smallest = Double.POSITIVE_INFINITY;
            double sum = 0;
            for (int i = 0; i < computed.length(); i++) {
                smallest = Math.min(smallest, computed.get(i));
                sum += computed.get(i);
            }
            if (!(smallest >= -epsilon && Math.abs(sum - 1) <= epsilon)) {
                throw new UnstablePredictionDistributionException();
            }

            final double shift = Math.max(0, -smallest);
            final double shiftedSum = sum + shift * computed.length();
            final double[] entries = new double[computed.length()];
            for (int i = 0; i < entries.length; i++) {
                entries[i] = (computed.get(i) + shift) / shiftedSum;
            }
            wellDefined = new Vector(entries);
        }
        return wellDefined;
    }
    // Checks that the prediction logic has valid dependencies.
    private void checkPredictionLogic() {
        Preconditions.checkState(
            diagonalization != null,
            "Diagonalization must be initialized"
        );
        Preconditions.checkState(
            diagonalization.P().rows() == HealthState.values().length,
            "The diagonalization must have one row and column per health state"
        );
        Preconditions.checkState(
            pLogic != null,
            "Matrix logic for P must be initialized"
        );
    }

}
