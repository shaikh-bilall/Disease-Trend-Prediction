package domain;

import com.google.common.base.Preconditions;
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
     * Returns the state with a given label.
     *
     * @param label the label to look up, exactly as returned by {@link #label()}
     * @return the matching state
     * @throws NullPointerException     if {@code label} is {@code null}
     * @throws IllegalArgumentException if no state has that label
     */
    public static HealthState fromLabel(
            final String label
    ) {
        Preconditions.checkNotNull(label, "Label must not be null");
        final HealthState state = BY_LABEL.get(label);
        Preconditions.checkArgument(state != null, "Unknown health state label: %s", label);
        return state;
    }

}