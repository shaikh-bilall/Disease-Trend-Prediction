package diseaseTrendPrediction.io.transitionDataStorage;

import com.google.common.base.Preconditions;
import domain.Diagonalization;
import domain.Matrix;

import java.time.Instant;

/**
 * Holds a complete transition snapshot and its source timestamp.
 *
 * @param transition transition matrix
 * @param diagonalization transition matrix diagonalization
 * @param sourceTimestamp source data timestamp
 */
public record TransitionData(
        Matrix transition,
        Diagonalization diagonalization,
        Instant sourceTimestamp
) {

    /**
     * Creates a transition data snapshot.
     *
     * @param transition transition matrix
     * @param diagonalization transition matrix diagonalization
     * @param sourceTimestamp source data timestamp
     */
    public TransitionData {
        Preconditions.checkNotNull(
                transition,
                "Transition matrix must not be null"
        );
        Preconditions.checkNotNull(
                diagonalization,
                "Diagonalization must not be null"
        );
        Preconditions.checkNotNull(
                sourceTimestamp,
                "Source timestamp must not be null"
        );
        transition = transition.copy();
        diagonalization = new Diagonalization(
                diagonalization.P(),
                diagonalization.D(),
                diagonalization.inverseP()
        );
        checkTransitionData();
    }

    /**
     * Returns a copy of the transition matrix.
     *
     * @return transition matrix copy
     */
    @Override
    public Matrix transition() {
        return transition.copy();
    }

    /**
     * Returns a copy of the diagonalization.
     *
     * @return diagonalization copy
     */
    @Override
    public Diagonalization diagonalization() {
        return new Diagonalization(
                diagonalization.P(),
                diagonalization.D(),
                diagonalization.inverseP()
        );
    }

    // Checks that this snapshot contains all required values.
    private void checkTransitionData() {
        Preconditions.checkNotNull(
                transition,
                "Transition matrix must not be null"
        );
        Preconditions.checkNotNull(
                diagonalization,
                "Diagonalization must not be null"
        );
        Preconditions.checkNotNull(
                sourceTimestamp,
                "Source timestamp must not be null"
        );
    }
}
