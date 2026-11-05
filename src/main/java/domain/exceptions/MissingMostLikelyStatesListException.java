package domain.exceptions;

import domain.PredictionResult;

/**
 * Thrown when the {@link PredictionResult} constructor receives {@code null}
 * in place of the list of most likely states.
 */
public final class MissingMostLikelyStatesListException extends NullPointerException{
}
