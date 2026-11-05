package domain.exceptions;

import domain.PredictionResult;

/**
 * Thrown when a {@link PredictionResult} is constructed with a
 * {@code null} predicted distribution.
 */
public final class NullPredictedDistributionException extends NullPointerException{
}
