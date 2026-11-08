package diseaseTrendPrediction.io.transitionDataStorage;

import com.google.common.base.Preconditions;
import domain.Diagonalization;
import domain.Matrix;

import java.time.Instant;

/** Holds a complete transition snapshot and its source timestamp. */
public record TransitionData(
        Matrix transition,
        Diagonalization diagonalization,
        Instant sourceTimestamp
) {

    public TransitionData {
        Preconditions.checkNotNull(transition, "Transition matrix must not be null");
        Preconditions.checkNotNull(diagonalization, "Diagonalization must not be null");
        Preconditions.checkNotNull(sourceTimestamp, "Source timestamp must not be null");
        transition = transition.copy();
        diagonalization = new Diagonalization(
                diagonalization.P(),
                diagonalization.D(),
                diagonalization.inverseP()
        );
        checkTransitionData();
    }

    @Override
    public Matrix transition() {
        return transition.copy();
    }

    @Override
    public Diagonalization diagonalization() {
        return new Diagonalization(
                diagonalization.P(),
                diagonalization.D(),
                diagonalization.inverseP()
        );
    }

    private void checkTransitionData() {
        Preconditions.checkNotNull(transition, "Transition matrix must not be null");
        Preconditions.checkNotNull(diagonalization, "Diagonalization must not be null");
        Preconditions.checkNotNull(sourceTimestamp, "Source timestamp must not be null");
    }
}
