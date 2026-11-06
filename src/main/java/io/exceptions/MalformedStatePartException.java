package io.exceptions;

/**
 * Thrown when a part of a state description is not a plain non-negative number, a percent sign and a state name, or
 * when its number is too large to be represented.
 */
public final class MalformedStatePartException extends IllegalArgumentException {
}