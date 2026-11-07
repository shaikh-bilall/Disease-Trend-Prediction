package diseaseTrendPrediction.logic;

import com.google.common.base.Preconditions;
import diseaseTrendPrediction.domain.HealthState;
import diseaseTrendPrediction.domain.MarkovModel;
import domain.Matrix;
import diseaseTrendPrediction.domain.PatientHistory;
import diseaseTrendPrediction.logic.exceptions.EmptyPatientHistoriesException;

import java.util.List;

/**
 * Estimates a Markov model from observed transitions between health states.
 */
public final class TransitionMatrixBuilder {

    /** Builds a transition model from patient histories. */
    public static MarkovModel build(
            final List<PatientHistory> patientHistories
    )
            throws EmptyPatientHistoriesException
    {
        Preconditions.checkNotNull(patientHistories, "Patient histories must not be null");
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
            Preconditions.checkNotNull(history, "Patient history must not be null");

            final List<HealthState> states = history.stateHistory();
            if (states.size() == 1) {
                final int only = states.getFirst().stateIndex();
                counts[only][only]++;
            }
            for (int i = 1; i < states.size(); i++) {
                counts[states.get(i).stateIndex()][states.get(i - 1).stateIndex()]++;
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
