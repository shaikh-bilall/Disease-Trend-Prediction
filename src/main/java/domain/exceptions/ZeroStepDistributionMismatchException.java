package domain.exceptions;

import domain.PredictionResult;

/**
 * Thrown when a zero-step {@link PredictionResult} has different initial
 * and final probabilities.
 */
public final class ZeroStepDistributionMismatchException extends IllegalStateException{
}
