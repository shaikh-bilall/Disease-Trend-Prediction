package domain;

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
     * Creates a patient history, keeping an independent copy of the states.
     *
     * @param patientId    the identifier of the patient, which must not be negative
     * @param stateHistory the patient's states in time order, with at least one entry
     * @throws NullPointerException  if {@code stateHistory} or any of its elements is {@code null}
     * @throws IllegalStateException if {@code patientId} is negative or {@code stateHistory} is empty
     */
    public PatientHistory(
            final int patientId,
            final List<HealthState> stateHistory
    ) {
        Preconditions.checkNotNull(stateHistory, "State history must not be null");

        this.patientId = patientId;
        this.stateHistory = List.copyOf(stateHistory);
        checkPatientHistory();
    }

    // Checks that the history is valid: a non-negative patient id and at least one state.
    private void checkPatientHistory() {
        Preconditions.checkNotNull(stateHistory, "State history must not be null");
        Preconditions.checkState(patientId >= 0, "Patient id must be non-negative");
        Preconditions.checkState(!stateHistory.isEmpty(), "State history must have at least one entry");
    }

}