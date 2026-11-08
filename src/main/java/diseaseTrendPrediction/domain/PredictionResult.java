package diseaseTrendPrediction.domain;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/** Represents the outcome of a health-state prediction. */
public record PredictionResult(
        int steps,
        StateVector initial,
        StateVector result,
        List<HealthState> mostLikelyStates
) {

    /** Creates a prediction result from its distributions and likely states. */
    public PredictionResult(
            final int steps,
            final StateVector initial,
            final StateVector result,
            final List<HealthState> mostLikelyStates
    ) {
        Preconditions.checkNotNull(initial, "Initial distribution must not be null");
        Preconditions.checkNotNull(result, "Result distribution must not be null");
        Preconditions.checkNotNull(mostLikelyStates, "Most likely states must not be null");
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

    @Override
    public StateVector initial() {
        return new StateVector(initial.vector(), initial.epsilon());
    }

    @Override
    public StateVector result() {
        return new StateVector(result.vector(), result.epsilon());
    }

    @Override
    public List<HealthState> mostLikelyStates() {
        return List.copyOf(new ArrayList<>(mostLikelyStates));
    }

    // Checks that distributions, steps and most likely states are all valid.
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
                steps != 0 || initial.vector().equals(result.vector()),
                "Initial and result distributions must match when steps are zero"
        );
    }

}
