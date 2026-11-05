package domain.exceptions;

import domain.HealthState;

/**
 * Thrown by {@link HealthState#fromLabel(String)} when the supplied
 * health state label is {@code null}.
 */
public final class NullHealthStateLabelException extends NullPointerException {
}
