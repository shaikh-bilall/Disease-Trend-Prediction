package diseaseTrendPrediction.domain;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/** Represents the observed health states of one patient. */
public record PatientHistory(int patientId, List<HealthState> stateHistory) {

    /** Creates a history for a patient. */
    public PatientHistory(
            final int patientId,
            final List<HealthState> stateHistory
    ) {
        Preconditions.checkNotNull(stateHistory, "State history must not be null");

        this.patientId = patientId;
        this.stateHistory = List.copyOf(stateHistory);

        checkPatientHistory();
    }

    @Override
    public List<HealthState> stateHistory() {
        return List.copyOf(new ArrayList<>(stateHistory));
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
