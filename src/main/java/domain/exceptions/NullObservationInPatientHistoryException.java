package domain.exceptions;

import domain.PatientHistory;

/**
 * Thrown when the state history list supplied to {@link PatientHistory}
 * contains a {@code null} entry. The constructor currently detects this
 * through {@link java.util.List#copyOf(java.util.Collection)}.
 */
public final class NullObservationInPatientHistoryException extends NullPointerException {
}
