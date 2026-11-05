package domain.exceptions;

import domain.PredictionResult;

/**
 * Thrown when the list of most likely states supplied to the
 * {@link PredictionResult} constructor contains a {@code null} entry.
 */
public final class NullEntryInMostLikelyStatesException extends NullPointerException{
}
