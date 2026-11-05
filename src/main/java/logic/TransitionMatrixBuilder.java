package logic;

import domain.Matrix;
import com.google.common.base.Preconditions;
import domain.HealthState;
import domain.MarkovModel;
import domain.PatientHistory;

import java.util.List;

/**
 * Estimates the Markov model from the patients' observed health states. Every move between two consecutive
 * observations of a patient is counted, and each column of counts is turned into probabilities of leaving that state.
 */
public final class TransitionMatrixBuilder {

    /**
     * Builds the model from patient histories. A patient with several entries contributes each move between
     * consecutive states; a patient with a single entry contributes one stay in that state. A state with no counts at
     * all is treated as absorbing, so it stays where it is with probability 1.
     *
     * @param patientHistories the histories of all patients
     * @return the column-stochastic model, ordered like {@code HealthState.values()}
     * @throws NullPointerException     if {@code patientHistories} or any of its elements is {@code null}
     * @throws IllegalArgumentException if {@code patientHistories} is empty
     */
    public static MarkovModel build(
            final List<PatientHistory> patientHistories
    ) {
        Preconditions.checkNotNull(patientHistories, "Patient histories must not be null");
        Preconditions.checkArgument(!patientHistories.isEmpty(), "Patient histories must not be empty");

        return new MarkovModel(new Matrix(normalizeColumns(countTransitions(patientHistories))));
    }

    // counts[to][from]: the moves between consecutive states, plus one stay for a patient seen only once.
    private static double[][] countTransitions(
            final List<PatientHistory> patientHistories
    ) {
        final int size = HealthState.values().length;
        final double[][] counts = new double[size][size];
        for (PatientHistory history : patientHistories) {
            Preconditions.checkNotNull(history, "Patient history must not be null");
            final List<HealthState> states = history.stateHistory();
            if (states.size() == 1) {
                final int only = states.get(0).ordinal();
                counts[only][only]++;
            }
            for (int i = 1; i < states.size(); i++) {
                counts[states.get(i).ordinal()][states.get(i - 1).ordinal()]++;
            }
        }
        return counts;
    }
    // Divides every column by its total; a column with no counts becomes absorbing, with 1 on the diagonal.
    private static double[][] normalizeColumns(
            final double[][] counts
    ) {
        final int size = counts.length;
        final double[][] probabilities = new double[size][size];
        for (int from = 0; from < size; from++) {
            double total = 0;
            for (int to = 0; to < size; to++) {
                total += counts[to][from];
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