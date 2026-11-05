package domain.exceptions;

import domain.HealthState;

/**
 * Thrown by {@link HealthState#fromLabel(String)} when the supplied
 * label does not exactly match any label returned by
 * {@link HealthState#label()}. Matching is case-sensitive.
 */
public final class UnknownHealthStateLabelException extends IllegalArgumentException{
}
