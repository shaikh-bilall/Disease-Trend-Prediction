package domain.exceptions;

import domain.PredictionResult;

/**
 * Thrown when a {@link PredictionResult} is constructed with a
 * {@code null} initial distribution.
 */
public final class NullInitialDistributionException extends NullPointerException{
}
