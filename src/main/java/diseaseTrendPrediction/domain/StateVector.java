package diseaseTrendPrediction.domain;

import com.google.common.base.Preconditions;
import domain.Vector;

import java.util.ArrayList;
import java.util.List;

/** Represents a probability distribution over health states. */
public record StateVector(Vector vector) {

    private static final HealthState[] STATES = HealthState.values();
    private static final double SUM_TOLERANCE = 1e-9;
    private static final double TIE_TOLERANCE = 1e-9;

    /** Creates a state vector from probability values. */
    public StateVector(
            final Vector vector
    ) {
        Preconditions.checkNotNull(vector, "State vector must not be null");

        this.vector = new Vector(vector.data());
        checkStateVector();
    }
    /** Returns the health states most likely in this distribution. */
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
    /** Returns the probability values in this distribution. */
    @Override
    public Vector vector() {
        return new Vector(vector.data());
    }
    // Checks the supplied vector for validity and throws an exception if it is invalid.
    private void checkStateVector() {
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
    /** Checks whether values form a valid health-state distribution. */
    public static boolean isDistribution(
            final Vector vector
    )
            throws IllegalStateException
    {
        Preconditions.checkNotNull(vector, "State vector must not be null");

        return (
            hasOneEntryPerState(vector) &&
            hasNoNegativeProbabilities(vector) &&
            sumsToOne(vector)
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
        boolean hasNegative = true;
        for (int i = 0; i < STATES.length; i++) {
            if (vector.get(i) < 0.0) {
                hasNegative = false;
            }
        }
        return hasNegative;
    }
    // Checks if the sum of probabilities in the vector is approximately equal to 1.0.
    private static boolean sumsToOne(
            final Vector vector
    ) {
        double sum = 0.0;
        for (int i = 0; i < STATES.length; i++) {
            sum += vector.get(i);
        }
        return (Math.abs(sum - 1.0) <= SUM_TOLERANCE);
    }

}
