package domain;

import domain.exceptions.*;
import com.google.common.base.Preconditions;

import java.util.List;

/**
 * The outcome of a prediction: the distribution over the health states after a number of time steps, next to the one
 * it started from, and the state or states most likely to hold.
 *
 * @param steps            the number of time steps between the initial distribution and the result
 * @param initial          the distribution at the start
 * @param result           the distribution after {@code steps} steps
 * @param mostLikelyStates every state tied for the highest probability in {@code result}, in state order
 */
public record PredictionResult(
        int steps,
        StateVector initial,
        StateVector result,
        List<HealthState> mostLikelyStates
) {

    /**
     * Creates a prediction result with both distributions
     * and an immutable copy of the list of most likely states.
     *
     * @param steps the non-negative number of prediction steps
     * @param initial the distribution at the start
     * @param result the distribution after {@code steps} steps
     * @param mostLikelyStates the states tied for the highest probability, in state order
     * @throws NullInitialDistributionException if {@code initial} is {@code null}
     * @throws NullPredictedDistributionException if {@code result} is {@code null}
     * @throws MissingMostLikelyStatesListException if {@code mostLikelyStates} is {@code null}
     * @throws NullEntryInMostLikelyStatesException if the list contains a {@code null} entry
     * @throws ZeroStepDistributionMismatchException if {@code steps} is {@code 0} and the
     * two distributions differ
     * @throws IllegalStateException if {@code steps} is negative, either distribution
     *         is invalid, or the list differs from {@link StateVector#mostLikelyStates()}
     *         for the predicted distribution
     */
    public PredictionResult(
            final int steps,
            final StateVector initial,
            final StateVector result,
            final List<HealthState> mostLikelyStates
    ) {
        if (initial == null) {
            throw new NullInitialDistributionException();
        }
        if (result == null) {
            throw new NullPredictedDistributionException();
        }
        if (mostLikelyStates == null) {
            throw new MissingMostLikelyStatesListException();
        }
        if (steps == 0) {
            if (!initial.vector().equals(result.vector())) {
                throw new ZeroStepDistributionMismatchException();
            }
        }

        this.steps = steps;
        this.initial = initial;
        this.result = result;
        try {
            this.mostLikelyStates = List.copyOf(mostLikelyStates);
        } catch (NullPointerException e) {
            throw new NullEntryInMostLikelyStatesException();
        }

        checkPredictionResult();
    }

    // Checks that the result is valid: steps that make sense, and the right most likely states for the result.
    private void checkPredictionResult() {
        Preconditions.checkNotNull(initial, "Initial distribution must not be null");
        Preconditions.checkNotNull(result, "Result distribution must not be null");
        Preconditions.checkNotNull(mostLikelyStates, "Most likely states must not be null");
        Preconditions.checkState(steps >= 0, "Steps must be non-negative");
        Preconditions.checkState(mostLikelyStates.equals(result.mostLikelyStates()),
                "Most likely states must be the states tied for the highest probability, in state order");
    }

}