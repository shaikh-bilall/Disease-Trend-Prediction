package diseaseTrendPrediction.domain;

import com.google.common.base.Preconditions;
import diseaseTrendPrediction.domain.exceptions.NullHealthStateLabelException;
import diseaseTrendPrediction.domain.exceptions.UnknownHealthStateLabelException;
import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.Map;

/** Represents a health state used by the disease model. */
public enum HealthState {
    HEALTHY("healthy"),
    INFECTED("infected"),
    RECOVERED("recovered");

    private static final Map<String, HealthState> BY_LABEL = Maps.uniqueIndex(
            Arrays.asList(values()),
            HealthState::label
    );
    private final String label;
    // Creates a health state with its display label.
    private HealthState(
            final String label
    ) {
        this.label = label;
    }
    /**
     * Returns the textual label for this health state.
     *
     * @return state label
     */
    public String label() {
        checkHealthState();
        return label;
    }
    /**
     * Returns the index used for this state in model data.
     *
     * @return state index
     */
    public int stateIndex() {
        checkHealthState();
        return ordinal();
    }

    // Checks that this state has a label.
    private void checkHealthState() {
        Preconditions.checkNotNull(
            label,
            "Health state label must be initialized"
        );
    }
    /**
     * Finds the health state represented by a label.
     *
     * @param label state label
     * @return matching health state
     * @throws NullHealthStateLabelException if the label is null
     * @throws UnknownHealthStateLabelException if the label is not recognized
     */
    public static HealthState fromLabel(
            final String label
    )
            throws NullHealthStateLabelException,
            UnknownHealthStateLabelException
    {
        if (label == null) {
            throw new NullHealthStateLabelException();
        }

        final HealthState state = BY_LABEL.get(label);
        if (state == null) {
            throw new UnknownHealthStateLabelException();
        }
        return state;
    }

}
