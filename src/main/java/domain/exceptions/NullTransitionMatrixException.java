package domain.exceptions;


import domain.MarkovModel;

/**
 * Thrown when a {@link MarkovModel} is constructed with a {@code null}
 * transition matrix.
 */
public final class NullTransitionMatrixException extends NullPointerException {
}
