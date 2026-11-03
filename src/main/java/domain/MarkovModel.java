package domain;

import ca.umanitoba.cs.shaikhb2.domain.Matrix;
import com.google.common.base.Preconditions;

import java.util.HashSet;
import java.util.List;

/**
 * A time-homogeneous Markov model: the names of its states and the transition matrix between them. Entry
 * {@code (i, j)} of the matrix is the probability of moving from state {@code j} to state {@code i} in one time
 * step, so every column sums to 1 and the matrix acts on a column state vector as {@code A·v}. State {@code i} is
 * the one named {@code states.get(i)}.
 *
 * @param states the state names, in the order of the matrix rows and columns
 * @param model  the column-stochastic transition matrix
 */
public record MarkovModel(List<String> states, Matrix model) {

    // How far a column sum may be from 1 and still count as 1.
    private static final double COLUMN_SUM_TOLERANCE = 1e-9;

    /**
     * Creates a model from state names and a transition matrix, keeping independent copies of both.
     *
     * @param states the state names, in the order of the matrix rows and columns
     * @param model  the column-stochastic transition matrix
     * @throws NullPointerException  if {@code states}, {@code model} or any state name is {@code null}
     * @throws IllegalStateException if the matrix is not square, the number of states differs from its size, a
     *                               state name is blank or repeated, an entry is negative, or a column does not
     *                               sum to 1
     */
    public MarkovModel(
            final List<String> states,
            final Matrix model
    ) {
        Preconditions.checkNotNull(states, "States must not be null");
        Preconditions.checkNotNull(model, "Model must not be null");

        this.states = List.copyOf(states);
        this.model = model.copy();

        checkMarkovModel();
    }

    // Checks that the model is valid: matching sizes, distinct named states, and a column-stochastic square matrix.
    private void checkMarkovModel() {
        Preconditions.checkNotNull(states, "States must not be null");
        Preconditions.checkNotNull(model, "Model must not be null");
        Preconditions.checkState(model.rows() == model.cols(), "Model must be square");
        Preconditions.checkState(states.size() == model.rows(), "Number of states must match the model size");
        Preconditions.checkState(states.stream().noneMatch(String::isBlank), "State names must not be blank");
        Preconditions.checkState(new HashSet<>(states).size() == states.size(), "State names must be unique");
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