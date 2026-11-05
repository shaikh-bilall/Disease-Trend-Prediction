package domain.exceptions;

import domain.StateVector;
import domain.Vector;

/**
 * Thrown when validation by the {@link StateVector} constructor or
 * {@link StateVector#mostLikelyStates()} finds a probability below zero.
 * {@link StateVector#isDistribution(Vector)} returns {@code false}
 * for this condition.
 */
public final class NegativeStateProbabilityException extends IllegalStateException {
}
