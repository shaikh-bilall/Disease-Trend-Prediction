package diseaseTrendPrediction.domain;

import com.google.common.base.Preconditions;
import linearAlgebra.domain.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a probability distribution over health states.
 *
 * @param vector probability values
 * @param epsilon tolerance used for validation and ties
 */
public record StateVector(
        Vector vector,
        double epsilon
) {

    private static final HealthState[] STATES = HealthState.values();

    /**
     * Creates a state vector from probability values and an injected tolerance.
     *
     * @param vector probability values
     * @param epsilon tolerance for validation and ties
     */
    public StateVector(
            final Vector vector,
            final double epsilon
    ) {
        Preconditions.checkNotNull(
            vector,
            "State vector must not be null"
        );
        Preconditions.checkArgument(
            Double.isFinite(epsilon) &&
            epsilon >= 0,
            "Epsilon must be finite and non-negative"
        );
        this.vector = new Vector(vector.data());
        this.epsilon = epsilon;
        checkStateVector();
    }

    /**
     * Returns the health states most likely in this distribution.
     *
     * @return immutable list of tied most likely states
     */
    public List<HealthState> mostLikelyStates() {
        checkStateVector();
        double highest = 0.0;
        for (int i = 0; i < STATES.length; i++) {
            highest = Math.max(highest, vector.get(i));
        }
        final List<HealthState> tied = new ArrayList<>();
        for (int i = 0; i < STATES.length; i++) {
            if (vector.get(i) >= highest - epsilon) {
                tied.add(STATES[i]);
            }
        }
        return List.copyOf(tied);
    }

    /**
     * Returns the probability values in this distribution.
     *
     * @return copy of the probability vector
     */
    @Override
    public Vector vector() {
        checkStateVector();
        return new Vector(vector.data());
    }

    /**
     * Returns the tolerance used to validate this distribution.
     *
     * @return distribution validation tolerance
     */
    @Override
    public double epsilon() {
        checkStateVector();
        return epsilon;
    }

    // Checks that this state vector is valid.
    private void checkStateVector() {
        Preconditions.checkState(
            vector.length() == STATES.length,
            "State vector must have one entry per health state"
        );
        for (int i = 0; i < STATES.length; i++) {
            Preconditions.checkState(
                vector.get(i) >= 0.0,
                "State vector must not have negative probabilities"
            );
        }
        Preconditions.checkState(
            sumsToOne(vector, epsilon),
            "State vector probabilities must sum to 1 within the sum tolerance"
        );
    }

    /**
     * Checks whether values form a valid health-state distribution.
     *
     * @param vector probability values
     * @param epsilon tolerance for the sum
     * @return whether the values form a distribution
     */
    public static boolean isDistribution(
            final Vector vector,
            final double epsilon
    )
    {
        Preconditions.checkNotNull(
            vector,
            "State vector must not be null"
        );
        Preconditions.checkArgument(
            Double.isFinite(epsilon) &&
            epsilon >= 0,
            "Epsilon must be finite and non-negative"
        );
        if (vector.length() != STATES.length) {
            return false;
        }
        for (int i = 0; i < STATES.length; i++) {
            if (vector.get(i) < 0.0) {
                return false;
            }
        }
        return sumsToOne(vector, epsilon);
    }

    // Checks whether the probabilities sum to one within epsilon.
    private static boolean sumsToOne(
            final Vector vector,
            final double epsilon
    ) {
        double sum = 0.0;
        for (int i = 0; i < STATES.length; i++) {
            sum += vector.get(i);
        }
        return Math.abs(sum - 1.0) <= epsilon;
    }
}
