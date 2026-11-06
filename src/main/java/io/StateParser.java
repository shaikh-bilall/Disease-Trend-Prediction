package io;

import domain.HealthState;
import domain.StateVector;
import domain.Vector;
import domain.exceptions.UnknownHealthStateLabelException;
import io.exceptions.DuplicateStateException;
import io.exceptions.EmptyStateDescriptionException;
import io.exceptions.InvalidPercentageSumException;
import io.exceptions.MalformedStatePartException;
import io.exceptions.NullStateDescriptionException;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a description of how patients are spread over the health states, such as
 * {@code 70% healthy, 20% infected, 10% recovered}, into a state vector. A state that is left out counts as 0%.
 */
public final class StateParser {

    private static final String PART_SEPARATOR = ",";
    // A plain number (digits, with an optional dot and more digits), a percent sign and a state name.
    private static final Pattern PART = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%\\s*(\\S+)");

    /**
     * Parses a description made of comma-separated parts, each written as a percentage and a state name, such as
     * {@code 70% healthy}. Spaces around the parts are ignored, and state names are not case-sensitive. The
     * percentages must add up to 100.
     *
     * @param description the description to parse
     * @return the state vector, with each percentage as a fraction and every state left out as 0
     * @throws NullStateDescriptionException    if {@code description} is {@code null}
     * @throws EmptyStateDescriptionException   if {@code description} is empty or only whitespace
     * @throws MalformedStatePartException      if a part is not a plain non-negative number, a percent sign and a
     *                                          state name, or its number is too large to be represented
     * @throws UnknownHealthStateLabelException if a state name is not the label of a health state
     * @throws DuplicateStateException          if a state is given more than once
     * @throws InvalidPercentageSumException    if the percentages do not add up to 100
     */
    public static StateVector parse(
            final String description
    )
            throws NullStateDescriptionException,
            EmptyStateDescriptionException,
            MalformedStatePartException,
            UnknownHealthStateLabelException,
            DuplicateStateException,
            InvalidPercentageSumException
    {
        if (description == null) {
            throw new NullStateDescriptionException();
        }
        if (description.isBlank()) {
            throw new EmptyStateDescriptionException();
        }

        final double[] fractions = new double[HealthState.values().length];
        final boolean[] given = new boolean[fractions.length];
        for (String part : description.split(PART_SEPARATOR, -1)) {
            final Matcher matcher = PART.matcher(part.strip());
            if (!matcher.matches()) {
                throw new MalformedStatePartException();
            }
            final double percentage = Double.parseDouble(matcher.group(1));
            if (!Double.isFinite(percentage)) {
                throw new MalformedStatePartException();
            }
            final HealthState state = HealthState.fromLabel(matcher.group(2).toLowerCase(Locale.ROOT));
            if (given[state.ordinal()]) {
                throw new DuplicateStateException();
            }
            given[state.ordinal()] = true;
            fractions[state.ordinal()] = percentage / 100;
        }

        final Vector vector = new Vector(fractions);
        if (!StateVector.isDistribution(vector)) {
            throw new InvalidPercentageSumException();
        }
        return new StateVector(vector);
    }

}