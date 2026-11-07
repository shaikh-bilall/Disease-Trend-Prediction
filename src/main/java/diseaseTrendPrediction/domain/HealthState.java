package diseaseTrendPrediction.domain;

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

    // Every state by its label.
    private static final Map<String, HealthState> BY_LABEL = Maps.uniqueIndex(
            Arrays.asList(values()),
            HealthState::label
    );
    private final String label;
    private HealthState(
            final String label
    ) {
        this.label = label;
    }
    /** Returns the textual label for this health state. */
    public String label() {
        return label;
    }
    public int stateIndex() {
        return ordinal();
    }
    /** Finds the health state represented by a label. */
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
