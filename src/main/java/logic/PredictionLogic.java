package logic;

import domain.Diagonalization;
import domain.Exceptions.MatrixMultiplyDimMismatchException;
import domain.Matrix;
import domain.Vector;

import com.google.common.base.Preconditions;

import domain.HealthState;
import domain.PredictionResult;
import domain.StateVector;
import domain.exceptions.NullInitialDistributionException;
import logic.exceptions.InvalidPredictionDiagonalizationSizeException;
import logic.exceptions.NegativePredictionStepsException;
import logic.exceptions.NullDiagonalizationException;
import logic.exceptions.PredictionMatrixDimensionMismatchException;
import logic.exceptions.UnstablePredictionDistributionException;

/**
 * Predicts how a distribution over the health states evolves. With the matrix diagonalized as {@code A = P D P^-1},
 * the matrix after {@code n} steps is {@code A^n = P D^n P^-1}, where {@code D^n} only raises the diagonal entries to
 * the power {@code n}. So the cost of a prediction does not grow with {@code n}, and the diagonalization is reused for
 * every prediction.
 */
public final class PredictionLogic {

    // How far below zero an entry of a computed distribution may be and still count as rounding noise.
    private static final double NEGATIVE_TOLERANCE = 1e-9;
    // How far the sum of a computed distribution may be from 1 and still count as rounding noise.
    private static final double SUM_TOLERANCE = 1e-6;

    private final Diagonalization diagonalization;
    // Multiplies by P on the left; built once because building it row-reduces P.
    private final MatrixLogic pLogic;

    /**
     * Creates the logic for a diagonalized transition matrix.
     *
     * @param diagonalization the factorization {@code A = P D P^-1} of a column-stochastic matrix, with one row and
     *                        column per health state
     * @throws NullDiagonalizationException if {@code diagonalization} is {@code null}
     * @throws InvalidPredictionDiagonalizationSizeException if the matrix size differs from the number of health states
     */
    public PredictionLogic(
            final Diagonalization diagonalization
    ) {
        if (diagonalization == null) {
            throw new NullDiagonalizationException();
        }
        if (diagonalization.P().rows() != HealthState.values().length) {
            throw new InvalidPredictionDiagonalizationSizeException();
        }

        this.diagonalization = diagonalization;
        this.pLogic = new MatrixLogic(diagonalization.P());
        checkPredictionLogic();
    }

    /**
     * Predicts the distribution over the health states after a number of time steps.
     *
     * @param initial the distribution now, with one entry per health state
     * @param steps   the number of time steps from now, which must not be negative
     * @return the initial distribution, the predicted one, and the most likely state or states
     * @throws NullInitialDistributionException if {@code initial} is {@code null}
     * @throws NegativePredictionStepsException if {@code steps} is negative
     * @throws IllegalStateException if {@code initial} is not a valid distribution
     * @throws PredictionMatrixDimensionMismatchException if the factor matrices cannot be multiplied
     * @throws UnstablePredictionDistributionException if the computed distribution falls outside the rounding tolerances
     */
    public PredictionResult predict(
            final Vector initial,
            final int steps
    ) {
        if (initial == null) {
            throw new NullInitialDistributionException();
        }
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
            final Matrix scaledInverse = new MatrixLogic(new Matrix(poweredData)).multiply(diagonalization.inverseP());
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
    // Checks that the diagonalization and matrix logic are initialized and have the right dimensions.
    private void checkPredictionLogic() {
        Preconditions.checkState(diagonalization != null, "Diagonalization must be initialized");
        Preconditions.checkState(diagonalization.P().rows() == HealthState.values().length, "The diagonalization must have one row and column per health state");
        Preconditions.checkState(pLogic != null, "Matrix logic for P must be initialized");
    }

}