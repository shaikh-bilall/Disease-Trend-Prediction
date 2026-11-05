package domain;

import com.google.common.base.Preconditions;
import domain.exceptions.NullStateVectorException;

import java.util.ArrayList;
import java.util.List;

/**
 * An immutable probability distribution over the health states. Each entry
 * corresponds to the health state at the same index in {@link HealthState#values()}.
 *
 * @param vector the probabilities in health state order
 */
public record StateVector(Vector vector) {

    private static final HealthState[] STATES = HealthState.values();
    private static final double SUM_TOLERANCE = 1e-9;
    private static final double TIE_TOLERANCE = 1e-9;

    /**
     * Creates a state vector from a snapshot of the supplied probabilities.
     *
     * @param vector the probabilities in health state order
     * @throws NullStateVectorException if {@code vector} is {@code null}
     * @throws IllegalStateException if {@code vector} does not have one entry
     * per health state, has negative probabilities, or does not
     * sum to {@code 1.0} within the sum tolerance
     */
    public StateVector(
            final Vector vector
    ) {
        if (vector == null) {
            throw new NullStateVectorException();
        }

        this.vector = new Vector(vector.data().clone());
        checkStateVector(this.vector);
    }

    /**
     * Returns an independent copy of the probabilities.
     *
     * @return a copy of the underlying vector
     */
    @Override
    public Vector vector() {
        return new Vector(vector.data().clone());
    }

    /**
     * Checks whether the supplied vector has one probability per health state,
     * has no negative probabilities, and sums to {@code 1.0} within the sum tolerance.
     *
     * @param vector the vector to test
     * @return whether the vector represents a valid distribution
     * @throws NullStateVectorException if {@code vector} is {@code null}
     * @throws IllegalStateException if the supplied vector's storage has been corrupted
     */
    public static boolean isDistribution(
            final Vector vector
    ) {
        if (vector == null) {
            throw new NullStateVectorException();
        }

        return (
            hasOneEntryPerState(vector) &&
            hasNoNegativeProbabilities(vector) &&
            sumsToOne(vector)
        );
    }

    /**
     * Returns the {@link HealthState}s whose probabilities are within the tie tolerance
     * of the highest probability.
     *
     * @return an immutable, non-empty list of the most likely states in alphabetical order
     */
    public List<HealthState> mostLikelyStates() {
        double highest = 0.0;
        for (int i = 0; i < STATES.length; i++) {
            highest = Math.max(highest, vector.get(i));
        }

        final List<HealthState> tied = new ArrayList<>();
        for (int i = 0; i < STATES.length; i++) {
            if (vector.get(i) >= highest - TIE_TOLERANCE) {
                tied.add(STATES[i]);
            }
        }
        return List.copyOf(tied);
    }

    // Checks the supplied vector for validity and throws an exception if it is invalid.
    private static void checkStateVector(
            final Vector vector
    ) {
        Preconditions.checkState(
                hasOneEntryPerState(vector),
                "State vector must have one entry per health state"
        );
        Preconditions.checkState(
                hasNoNegativeProbabilities(vector),
                "State vector must not have negative probabilities"
        );
        Preconditions.checkState(
                sumsToOne(vector),
                "State vector probabilities must sum to 1 within the sum tolerance"
        );
    }
    // Checks if the number of entries in the vector matches the number of health states.
    private static boolean hasOneEntryPerState(
            final Vector vector
    ) {
        return vector.length() == STATES.length;
    }
    // Checks if all probabilities in the vector are non-negative.
    private static boolean hasNoNegativeProbabilities(
            final Vector vector
    ) {
        for (int i = 0; i < STATES.length; i++) {
            if (vector.get(i) < 0.0) {
                return false;
            }
        }
        return true;
    }
    // Checks if the sum of probabilities in the vector is approximately equal to 1.0 within a specified tolerance.
    private static boolean sumsToOne(
            final Vector vector
    ) {
        double sum = 0.0;
        for (int i = 0; i < STATES.length; i++) {
            sum += vector.get(i);
        }
        return Math.abs(sum - 1.0) <= SUM_TOLERANCE;
    }
}