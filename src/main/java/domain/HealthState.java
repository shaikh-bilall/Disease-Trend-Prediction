package domain;

import domain.exceptions.NullHealthStateLabelException;
import domain.exceptions.UnknownHealthStateLabelException;
import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.Map;

/**
 * The health states a patient can be in. The constants are in alphabetical order, so a state's ordinal is its row and
 * column in the transition matrix and its entry in a state vector. Each state has a lowercase label, which is how the
 * state is written in the data and typed by the user.
 */
public enum HealthState {
    HEALTHY("healthy"),
    INFECTED("infected"),
    RECOVERED("recovered");

    // Every state by its label.
    private static final Map<String, HealthState> BY_LABEL = Maps.uniqueIndex(Arrays.asList(values()), HealthState::label);

    private final String label;

    HealthState(
            final String label
    ) {
        this.label = label;
    }

    /**
     * Returns the label of this state.
     *
     * @return the lowercase label
     */
    public String label() {
        return label;
    }
    /**
     * Returns the health state whose label exactly matches the supplied label.
     *
     * @param label the case-sensitive label to look up
     * @return the matching health state
     * @throws NullHealthStateLabelException if {@code label} is {@code null}
     * @throws UnknownHealthStateLabelException if no state has that label
     */
    public static HealthState fromLabel(
            final String label
    ) {
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