package domain;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * The outcome of a prediction: the distribution over the health states after a number of time steps, next to the one
 * it started from, and the state or states most likely to hold. Entry {@code i} of each distribution belongs to
 * {@code HealthState.values()[i]}.
 *
 * @param steps            the number of time steps between the initial distribution and the result
 * @param initial          the distribution at the start
 * @param result           the distribution after {@code steps} steps
 * @param mostLikelyStates every state tied for the highest probability in {@code result}, in state order
 */
public record PredictionResult(
        int steps,
        Vector initial,
        Vector result,
        List<HealthState> mostLikelyStates
) {

    // How far a distribution's sum may be from 1, and how far a probability may be from the highest, to still tie.
    private static final double TOLERANCE = 1e-9;

    /**
     * Creates a prediction result, keeping independent copies of the distributions and the list.
     *
     * @param steps            the number of time steps, which must not be negative
     * @param initial          the distribution at the start
     * @param result           the distribution after {@code steps} steps
     * @param mostLikelyStates every state tied for the highest probability in {@code result}, in state order
     * @throws NullPointerException  if any argument, or any element of {@code mostLikelyStates}, is {@code null}
     * @throws IllegalStateException if {@code steps} is negative, a distribution does not have one entry per health
     *                               state, has a negative entry or does not sum to 1, or {@code mostLikelyStates} is
     *                               not the states tied for the highest probability in {@code result}
     */
    public PredictionResult(
            final int steps,
            final Vector initial,
            final Vector result,
            final List<HealthState> mostLikelyStates
    ) {
        Preconditions.checkNotNull(initial, "Initial distribution must not be null");
        Preconditions.checkNotNull(result, "Result distribution must not be null");
        Preconditions.checkNotNull(mostLikelyStates, "Most likely states must not be null");

        this.steps = steps;
        this.initial = new Vector(initial.data());
        this.result = new Vector(result.data());
        this.mostLikelyStates = List.copyOf(mostLikelyStates);

        checkPredictionResult();
    }

    // Checks that the result is valid: matching sizes, two probability distributions, and the right most likely states.
    private void checkPredictionResult() {
        Preconditions.checkNotNull(initial, "Initial distribution must not be null");
        Preconditions.checkNotNull(result, "Result distribution must not be null");
        Preconditions.checkNotNull(mostLikelyStates, "Most likely states must not be null");
        Preconditions.checkState(steps >= 0, "Steps must be non-negative");
        Preconditions.checkState(initial.length() == HealthState.values().length, "Initial distribution must have one entry per health state");
        Preconditions.checkState(result.length() == HealthState.values().length, "Result distribution must have one entry per health state");
        checkDistribution(initial, "Initial distribution");
        checkDistribution(result, "Result distribution");
        Preconditions.checkState(mostLikelyStates.equals(getTiedStates()),
                "Most likely states must be the states tied for the highest probability, in state order");
    }
    // Checks that the distribution has no negative entry and sums to 1.
    private void checkDistribution(
            final Vector distribution,
            final String name
    ) {
        double sum = 0;
        for (int i = 0; i < distribution.length(); i++) {
            final double entry = distribution.get(i);
            Preconditions.checkState(entry >= 0, "%s must not have negative entries", name);
            sum += entry;
        }
        Preconditions.checkState(Math.abs(sum - 1) <= TOLERANCE, "%s must sum to 1", name);
    }
    // The states whose probability in the result is within the tolerance of the highest, in state order.
    private List<HealthState> getTiedStates() {
        double highest = 0;
        for (int i = 0; i < result.length(); i++) {
            highest = Math.max(highest, result.get(i));
        }
        final List<HealthState> tied = new ArrayList<>();
        for (int i = 0; i < result.length(); i++) {
            if (result.get(i) >= highest - TOLERANCE) {
                tied.add(HealthState.values()[i]);
            }
        }
        return tied;
    }

}