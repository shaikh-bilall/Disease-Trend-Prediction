package domain;

import domain.exceptions.NullTransitionMatrixException;
import com.google.common.base.Preconditions;

/**
 * A time-homogeneous Markov model over the health states. Entry {@code (i, j)} of the matrix is the probability of
 * moving from state {@code j} to state {@code i} in one time step, so every column sums to 1 and the matrix acts on a
 * column state vector as {@code A·v}. Row and column {@code i} belong to {@code HealthState.values()[i]}.
 *
 * @param model the column-stochastic transition matrix
 */
public record MarkovModel(Matrix model) {

    // How far a column sum may be from 1 and still count as 1.
    private static final double COLUMN_SUM_TOLERANCE = 1e-9;

    /**
     * Creates a Markov model from an independent copy of the transition matrix.
     *
     * @param model the column-stochastic transition matrix
     * @throws NullTransitionMatrixException if {@code model} is {@code null}
     * @throws IllegalStateException if the matrix is not square, its size differs
     *         from the number of health states, an entry is negative or NaN,
     *         or a column sum falls outside the tolerance around {@code 1.0}
     */
    public MarkovModel(
            final Matrix model
    ) {
        if (model == null) {
            throw new NullTransitionMatrixException();
        }

        this.model = model.copy();
        checkMarkovModel();
    }

    // Checks that the model is valid: one row and column per health state, and a column-stochastic matrix.
    private void checkMarkovModel() {
        Preconditions.checkNotNull(model, "Model must not be null");
        Preconditions.checkState(model.rows() == model.cols(), "Model must be square");
        Preconditions.checkState(model.rows() == HealthState.values().length, "Model size must match the number of health states");
        for (int col = 0; col < model.cols(); col++) {
            double sum = 0;
            for (int row = 0; row < model.rows(); row++) {
                final double entry = model.get(row, col);
                Preconditions.checkState(entry >= 0, "Model entries must be non-negative");
                sum += entry;
            }
            Preconditions.checkState(Math.abs(sum - 1) <= COLUMN_SUM_TOLERANCE,
                    "Column %s of the model must sum to 1", col);
        }
    }

}