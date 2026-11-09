package diseaseTrendPrediction.domain;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the observed health states of one patient.
 *
 * @param patientId patient identifier
 * @param stateHistory states observed in time order
 */
public record PatientHistory(
        int patientId,
        List<HealthState> stateHistory
) {

    /**
     * Creates a history for a patient.
     *
     * @param patientId patient identifier
     * @param stateHistory observed states in time order
     * @throws NullPointerException if stateHistory is null
     * @throws IllegalStateException if the identifier or history is invalid
     */
    public PatientHistory(
            final int patientId,
            final List<HealthState> stateHistory
    ) {
        Preconditions.checkNotNull(
                stateHistory,
                "State history must not be null"
        );

        this.patientId = patientId;
        this.stateHistory = List.copyOf(stateHistory);

        checkPatientHistory();
    }

    /**
     * Returns the observed states.
     *
     * @return immutable state history
     */
    @Override
    public List<HealthState> stateHistory() {
        return List.copyOf(new ArrayList<>(stateHistory));
    }

    // Checks that this patient history is valid.
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
