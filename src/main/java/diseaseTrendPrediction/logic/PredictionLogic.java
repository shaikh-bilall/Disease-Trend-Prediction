package diseaseTrendPrediction.logic;

import domain.Diagonalization;
import domain.Exceptions.MatrixMultiplyDimMismatchException;
import domain.Matrix;
import domain.Vector;

import com.google.common.base.Preconditions;

import diseaseTrendPrediction.domain.HealthState;
import diseaseTrendPrediction.domain.PredictionResult;
import diseaseTrendPrediction.domain.StateVector;
import diseaseTrendPrediction.logic.exceptions.InvalidPredictionDiagonalizationSizeException;
import diseaseTrendPrediction.logic.exceptions.NegativePredictionStepsException;
import diseaseTrendPrediction.logic.exceptions.PredictionMatrixDimensionMismatchException;
import diseaseTrendPrediction.logic.exceptions.UnstablePredictionDistributionException;
import logic.MatrixLogic;

/** Predicts future health-state distributions from a transition model. */
public final class PredictionLogic {

    // How far below zero an entry of a computed distribution may be and still count as rounding noise.
    private static final double NEGATIVE_TOLERANCE = 1e-9;
    // How far the sum of a computed distribution may be from 1 and still count as rounding noise.
    private static final double SUM_TOLERANCE = 1e-6;

    private final Diagonalization diagonalization;
    // Multiplies by P on the left; built once because building it row-reduces P.
    private final MatrixLogic pLogic;

    /** Initializes prediction logic for a health-state transition model. */
    public PredictionLogic(
            final Diagonalization diagonalization
    ) {
        Preconditions.checkNotNull(diagonalization, "Diagonalization must not be null");
        if (diagonalization.P().rows() != HealthState.values().length) {
            throw new InvalidPredictionDiagonalizationSizeException();
        }

        this.diagonalization = diagonalization;
        this.pLogic = new MatrixLogic(diagonalization.P());
        checkPredictionLogic();
    }

    /** Predicts a health-state distribution after a number of steps. */
    public PredictionResult predict(
            final Vector initial,
            final int steps
    )
            throws NegativePredictionStepsException,
            IllegalStateException,
            PredictionMatrixDimensionMismatchException,
            UnstablePredictionDistributionException
    {
        Preconditions.checkNotNull(initial, "Initial distribution must not be null");
        if (steps < 0) {
            throw new NegativePredictionStepsException();
        }
        final StateVector start = new StateVector(initial);
        if (steps == 0) {
            return new PredictionResult(0, start, start, start.mostLikelyStates());
        }

        final Vector computed = getTransitionPower(steps).multiply(start.vector());
        final StateVector result = new StateVector(getWellDefined(computed));
        return new PredictionResult(steps, start, result, result.mostLikelyStates());
    }
    // A^n = P (D^n P^-1), where D^n raises each eigenvalue on the diagonal of D to the n-th power.
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
    // Returns a valid distribution as-is. Within the rounding tolerances, shifts entries
    // up if needed to remove negatives, then normalizes their sum to 1.
    private Vector getWellDefined(
            final Vector computed
    ) {
        if (StateVector.isDistribution(computed)) {
            return computed;
        }

        double smallest = Double.POSITIVE_INFINITY;
        double sum = 0;
        for (int i = 0; i < computed.length(); i++) {
            smallest = Math.min(smallest, computed.get(i));
            sum += computed.get(i);
        }
        if (!(smallest >= -NEGATIVE_TOLERANCE && Math.abs(sum - 1) <= SUM_TOLERANCE)) {
            throw new UnstablePredictionDistributionException();
        }

        final double shift = Math.max(0, -smallest);
        final double shiftedSum = sum + shift * computed.length();
        final double[] entries = new double[computed.length()];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = (computed.get(i) + shift) / shiftedSum;
        }
        return new Vector(entries);
    }
    // Checks that the diagonalization and matrix logic are initialized and have the correct dimensions.
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
