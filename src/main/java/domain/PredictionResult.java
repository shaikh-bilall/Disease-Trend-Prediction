package domain;

import ca.umanitoba.cs.shaikhb2.domain.Vector;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * The outcome of a prediction: the distribution over the states after a number of time steps, next to the one it
 * started from, and the state or states most likely to hold. Entry {@code i} of each distribution belongs to the
 * state named {@code states.get(i)}.
 *
 * @param states           the state names, in the order of the distribution entries
 * @param steps            the number of time steps between the initial distribution and the result
 * @param initial          the distribution at the start
 * @param result           the distribution after {@code steps} steps
 * @param mostLikelyStates every state tied for the highest probability in {@code result}, in state order
 */
public record PredictionResult(
        List<String> states,
        int steps,
        Vector initial,
        Vector result,
        List<String> mostLikelyStates
) {

    // How far a distribution's sum may be from 1, and how far a probability may be from the highest, to still tie.
    private static final double TOLERANCE = 1e-9;

    /**
     * Creates a prediction result, keeping independent copies of the names and distributions.
     *
     * @param states           the state names, in the order of the distribution entries
     * @param steps            the number of time steps, which must not be negative
     * @param initial          the distribution at the start
     * @param result           the distribution after {@code steps} steps
     * @param mostLikelyStates every state tied for the highest probability in {@code result}, in state order
     * @throws NullPointerException  if any argument, or any state name, is {@code null}
     * @throws IllegalStateException if {@code steps} is negative, a distribution does not have one entry per state,
     *                               has a negative entry or does not sum to 1, or {@code mostLikelyStates} is not
     *                               the states tied for the highest probability in {@code result}
     */
    public PredictionResult(
            final List<String> states,
            final int steps,
            final Vector initial,
            final Vector result,
            final List<String> mostLikelyStates
    ) {
        Preconditions.checkNotNull(states, "States must not be null");
        Preconditions.checkNotNull(initial, "Initial distribution must not be null");
        Preconditions.checkNotNull(result, "Result distribution must not be null");
        Preconditions.checkNotNull(mostLikelyStates, "Most likely states must not be null");
        this.states = List.copyOf(states);
        this.steps = steps;
        this.initial = new Vector(initial.data());
        this.result = new Vector(result.data());
        this.mostLikelyStates = List.copyOf(mostLikelyStates);
        checkPredictionResult();
    }

    // Checks that the result is valid: matching sizes, two probability distributions, and the right most likely states.
    private void checkPredictionResult() {
        Preconditions.checkNotNull(states, "States must not be null");
        Preconditions.checkNotNull(initial, "Initial distribution must not be null");
        Preconditions.checkNotNull(result, "Result distribution must not be null");
        Preconditions.checkNotNull(mostLikelyStates, "Most likely states must not be null");
        Preconditions.checkState(steps >= 0, "Steps must be non-negative");
        Preconditions.checkState(initial.length() == states.size(), "Initial distribution must have one entry per state");
        Preconditions.checkState(result.length() == states.size(), "Result distribution must have one entry per state");
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
    private List<String> getTiedStates() {
        double highest = 0;
        for (int i = 0; i < result.length(); i++) {
            highest = Math.max(highest, result.get(i));
        }
        final List<String> tied = new ArrayList<>();
        for (int i = 0; i < result.length(); i++) {
            if (result.get(i) >= highest - TOLERANCE) {
                tied.add(states.get(i));
            }
        }
        return tied;
    }

}