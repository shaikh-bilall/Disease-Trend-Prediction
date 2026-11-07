package diseaseTrendPrediction.io;

import diseaseTrendPrediction.domain.HealthState;
import diseaseTrendPrediction.domain.StateVector;
import domain.Vector;
import diseaseTrendPrediction.domain.exceptions.UnknownHealthStateLabelException;
import diseaseTrendPrediction.io.exceptions.DuplicateStateException;
import diseaseTrendPrediction.io.exceptions.EmptyStateDescriptionException;
import diseaseTrendPrediction.io.exceptions.InvalidPercentageSumException;
import diseaseTrendPrediction.io.exceptions.MalformedStatePartException;
import diseaseTrendPrediction.io.exceptions.NullStateDescriptionException;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts a textual health-state distribution into a state vector. */
public final class StateParser {

    private static final String PART_SEPARATOR = ",";
    // A plain number (digits, with an optional dot and more digits), a percent sign and a state name.
    private static final Pattern PART = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%\\s*(\\S+)");

    /** Parses a textual description into a health-state distribution. */
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
            if (given[state.stateIndex()]) {
                throw new DuplicateStateException();
            }
            given[state.stateIndex()] = true;
            fractions[state.stateIndex()] = percentage / 100;
        }

        final Vector vector = new Vector(fractions);
        if (!StateVector.isDistribution(vector)) {
            throw new InvalidPercentageSumException();
        }
        return new StateVector(vector);
    }

}
