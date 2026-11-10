package diseaseTrendPrediction.domain;

import com.google.common.base.Preconditions;
import linearAlgebra.domain.Matrix;

/**
 * Represents transition probabilities between health states.
 *
 * @param model transition matrix
 * @param epsilon tolerance used to validate transition probabilities
 */
public record MarkovModel(
        Matrix model,
        double epsilon
) {

    /**
     * Creates a Markov model from transition probabilities.
     *
     * @param model transition probabilities
     * @param epsilon tolerance for column sum validation
     */
    public MarkovModel(
            final Matrix model,
            final double epsilon
    ) {
        Preconditions.checkNotNull(
            model,
            "Model must not be null"
        );
        Preconditions.checkArgument(
            Double.isFinite(epsilon) &&
            epsilon >= 0,
            "Epsilon must be finite and non-negative"
        );

        this.model = model.copy();
        this.epsilon = epsilon;
        checkMarkovModel();
    }

    /**
     * Returns a copy of the transition matrix.
     *
     * @return transition matrix copy
     */
    @Override
    public Matrix model() {
        checkMarkovModel();
        return new Matrix(model.data());
    }

    /**
     * Returns the tolerance used to validate this model.
     *
     * @return model validation tolerance
     */
    @Override
    public double epsilon() {
        checkMarkovModel();
        return epsilon;
    }

    // Checks that the model satisfies its invariants.
    private void checkMarkovModel() {
        Preconditions.checkNotNull(
            model,
            "Model must not be null"
        );
        Preconditions.checkState(
            model.rows() == model.cols(),
            "Model must be square"
        );
        Preconditions.checkState(
            model.rows() == HealthState.values().length,
            "Model size must match the number of health states"
        );
        for (int col = 0; col < model.cols(); col++) {
            double sum = 0;
            for (int row = 0; row < model.rows(); row++) {
                final double entry = model.get(row, col);
                Preconditions.checkState(
                    entry >= 0,
                    "Model entries must be non-negative"
                );
                sum += entry;
            }
            Preconditions.checkState(
                Math.abs(sum - 1) <= epsilon,
                "Column %s of the model must sum to 1",
                col
            );
        }
    }

}
