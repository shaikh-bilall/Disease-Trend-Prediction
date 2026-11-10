package diseaseTrendPrediction.domain;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the outcome of a health-state prediction.
 *
 * @param steps number of transitions applied
 * @param initial starting distribution
 * @param result predicted distribution
 * @param mostLikelyStates states tied for the highest predicted probability
 */
public record PredictionResult(
        int steps,
        StateVector initial,
        StateVector result,
        List<HealthState> mostLikelyStates
) {

    /**
     * Creates a prediction result from its distributions and likely states.
     *
     * @param steps prediction steps
     * @param initial starting distribution
     * @param result predicted distribution
     * @param mostLikelyStates states tied for the highest predicted probability
     */
    public PredictionResult(
            final int steps,
            final StateVector initial,
            final StateVector result,
            final List<HealthState> mostLikelyStates
    ) {
        Preconditions.checkNotNull(
            initial,
            "Initial distribution must not be null"
        );
        Preconditions.checkNotNull(
            result,
            "Result distribution must not be null"
        );
        Preconditions.checkNotNull(
            mostLikelyStates,
            "Most likely states must not be null"
        );
        Preconditions.checkArgument(
            initial.epsilon() == result.epsilon(),
            "Initial and result distributions must use the same epsilon"
        );

        this.steps = steps;
        this.initial = initial;
        this.result = result;
        this.mostLikelyStates = List.copyOf(mostLikelyStates);

        checkPredictionResult();
    }

    /**
     * Returns the starting distribution.
     *
     * @return starting distribution copy
     */
    @Override
    public StateVector initial() {
        checkPredictionResult();
        return new StateVector(initial.vector(), initial.epsilon());
    }

    /**
     * Returns the predicted distribution.
     *
     * @return predicted distribution copy
     */
    @Override
    public StateVector result() {
        checkPredictionResult();
        return new StateVector(result.vector(), result.epsilon());
    }

    /**
     * Returns the most likely health states.
     *
     * @return immutable list of likely states
     */
    @Override
    public List<HealthState> mostLikelyStates() {
        checkPredictionResult();
        return List.copyOf(new ArrayList<>(mostLikelyStates));
    }

    /**
     * Returns the number of transitions applied to produce this result.
     *
     * @return number of prediction steps
     */
    @Override
    public int steps() {
        checkPredictionResult();
        return steps;
    }

    // Checks that this prediction result is valid.
    private void checkPredictionResult() {
        Preconditions.checkNotNull(
            initial,
            "Initial distribution must not be null"
        );
        Preconditions.checkNotNull(
            result,
            "Result distribution must not be null"
        );
        Preconditions.checkNotNull(
            mostLikelyStates,
            "Most likely states must not be null"
        );
        Preconditions.checkState(
            steps >= 0,
            "Steps must be non-negative"
        );
        Preconditions.checkState(
            mostLikelyStates.equals(result.mostLikelyStates()),
            "states must be tied for the highest probability, in state order"
        );
        Preconditions.checkState(
            steps != 0 ||
            initial.vector().equals(result.vector()),
            "Initial and result distributions must match when steps are zero"
        );
    }

}
