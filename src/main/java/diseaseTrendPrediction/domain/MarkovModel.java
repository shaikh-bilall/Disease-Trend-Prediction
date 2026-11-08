package diseaseTrendPrediction.domain;

import com.google.common.base.Preconditions;
import domain.Matrix;

/** Represents transition probabilities between health states. */
public record MarkovModel(Matrix model, double epsilon) {

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
        Preconditions.checkNotNull(model, "Model must not be null");
        Preconditions.checkArgument(
                Double.isFinite(epsilon) && epsilon >= 0,
                "Epsilon must be finite and non-negative"
        );

        this.model = model.copy();
        this.epsilon = epsilon;
        checkMarkovModel();
    }

    @Override
    public Matrix model() {
        return new Matrix(model.data());
    }

    // Checks that the model has one row and column per health state, and a column-stochastic matrix.
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
                    "Column %s of the model must sum to 1", col
            );
        }
    }

}
