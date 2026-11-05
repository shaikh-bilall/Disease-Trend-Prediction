package domain.exceptions;

import domain.StateVector;
import domain.Vector;

/**
 * Thrown when {@code null} is passed as the underlying {@link Vector}
 * to the {@link StateVector} constructor or
 * {@link StateVector#isDistribution(Vector)}.
 */
public final class NullStateVectorException extends NullPointerException {
}
