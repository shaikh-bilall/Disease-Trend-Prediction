package domain;

import domain.exceptions.MissingPatientHistoryListException;
import domain.exceptions.NullObservationInPatientHistoryException;
import com.google.common.base.Preconditions;

import java.util.List;

/**
 * One patient's observed health states, in time order. Consecutive entries are one time step apart.
 *
 * @param patientId    the identifier of the patient
 * @param stateHistory the patient's states, from the earliest observation to the latest
 */
public record PatientHistory(int patientId, List<HealthState> stateHistory) {

    /**
     * Creates a patient history with an immutable copy of the observed states.
     *
     * @param patientId the non-negative patient identifier
     * @param stateHistory the observed states in time order, with at least one entry
     * @throws MissingPatientHistoryListException if {@code stateHistory} is {@code null}
     * @throws NullObservationInPatientHistoryException if the list contains a {@code null} entry
     * @throws IllegalStateException if {@code patientId} is negative or the history is empty
     */
    public PatientHistory(
            final int patientId,
            final List<HealthState> stateHistory
    ) {
        if (stateHistory == null) {
            throw new MissingPatientHistoryListException();
        }

        this.patientId = patientId;
        try {
            this.stateHistory = List.copyOf(stateHistory);
        } catch (NullPointerException e) {
            throw new NullObservationInPatientHistoryException();
        }

        checkPatientHistory();
    }

    // Checks that the history is valid: a non-negative patient id and at least one state.
    private void checkPatientHistory() {
        Preconditions.checkNotNull(
                stateHistory,
                "State history must not be null"
        );
        Preconditions.checkState(
                patientId >= 0,
                "Patient id must be non-negative"
        );
        Preconditions.checkState(
                !stateHistory.isEmpty(),
                "State history must have at least one entry"
        );
    }

}