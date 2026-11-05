package domain.exceptions;

import domain.PatientHistory;

/**
 * Thrown when a {@link PatientHistory} is constructed with a
 * {@code null} state history list.
 */
public final class MissingPatientHistoryListException extends NullPointerException {
}