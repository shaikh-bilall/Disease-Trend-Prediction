package domain.exceptions;

import domain.HealthState;
import domain.StateVector;
import domain.Vector;

/**
 * Thrown when validation by the {@link StateVector} constructor or
 * {@link StateVector#mostLikelyStates()} finds a vector whose length
 * differs from the number of {@link HealthState} constants.
 * {@link StateVector#isDistribution(Vector)} returns {@code false}
 * for this condition.
 */
public final class StateVectorStateCountMismatchException extends IllegalStateException{
}
