package domain.exceptions;

import domain.StateVector;
import domain.Vector;

/**
 * Thrown when validation by the {@link StateVector} constructor or
 * {@link StateVector#mostLikelyStates()} finds that the probabilities
 * fail the sum tolerance checks against {@code 1.0}.
 * {@link StateVector#isDistribution(Vector)} returns {@code false}
 * for this condition.
 */
public final class StateVectorProbabilitySumNotOneException extends IllegalStateException {
}
