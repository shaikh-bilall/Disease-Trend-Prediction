package logic;

import domain.HealthState;
import domain.MarkovModel;
import domain.Matrix;
import domain.PatientHistory;
import logic.exceptions.EmptyPatientHistoriesException;
import logic.exceptions.NullPatientHistoriesException;
import logic.exceptions.NullPatientHistoryException;

import java.util.List;

/**
 * Estimates a Markov model from observed transitions between health states.
 */
public final class TransitionMatrixBuilder {

    /**
     * Builds the model from patient histories. A patient with several observations
     * contributes each transition between consecutive observations. A patient with
     * one observation contributes one stay in that state. A state with no outgoing
     * counts is treated as absorbing.
     *
     * @param patientHistories the histories of all patients
     * @return the column-stochastic model in {@link HealthState#values()} order
     * @throws NullPatientHistoriesException if {@code patientHistories} is {@code null}
     * @throws EmptyPatientHistoriesException if {@code patientHistories} is empty
     * @throws NullPatientHistoryException if any history is {@code null}
     */
    public static MarkovModel build(
            final List<PatientHistory> patientHistories
    )
            throws NullPatientHistoriesException,
            EmptyPatientHistoriesException,
            NullPatientHistoryException
    {
        if (patientHistories == null) {
            throw new NullPatientHistoriesException();
        }
        if (patientHistories.isEmpty()) {
            throw new EmptyPatientHistoriesException();
        }

        return new MarkovModel(
                new Matrix(
                        normalizeColumns(countTransitions(patientHistories))
                )
        );
    }
    // Counts are indexed by destination state, then source state.
    private static double[][] countTransitions(
            final List<PatientHistory> patientHistories
    ) {
        final int size = HealthState.values().length;
        final double[][] counts = new double[size][size];

        for (PatientHistory history : patientHistories) {
            if (history == null) {
                throw new NullPatientHistoryException();
            }

            final List<HealthState> states = history.stateHistory();
            if (states.size() == 1) {
                final int only = states.getFirst().ordinal();
                counts[only][only]++;
            }
            for (int i = 1; i < states.size(); i++) {
                counts[states.get(i).ordinal()][states.get(i - 1).ordinal()]++;
            }
        }

        return counts;
    }
    // Produces a column-stochastic matrix of probabilities by normalizing the input matrix.
    private static double[][] normalizeColumns(
            final double[][] counts
    ) {
        final int size = counts.length;
        final double[][] probabilities = new double[size][size];

        for (int from = 0; from < size; from++) {
            double total = 0;
            for (double[] count : counts) {
                total += count[from];
            }
            if (total == 0) {
                probabilities[from][from] = 1;
                continue;
            }
            for (int to = 0; to < size; to++) {
                probabilities[to][from] = counts[to][from] / total;
            }
        }

        return probabilities;
    }

}