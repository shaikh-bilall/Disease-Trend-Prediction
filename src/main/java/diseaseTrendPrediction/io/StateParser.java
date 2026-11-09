package diseaseTrendPrediction.io;

import com.google.common.base.Preconditions;
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
    private static final Pattern PART = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%\\s*(\\S+)");

    /**
     * Parses a textual description into a health-state distribution.
     *
     * @param description distribution description
     * @param epsilon tolerance for validating the percentage sum
     * @return parsed state vector
     * @throws NullStateDescriptionException if description is null
     * @throws EmptyStateDescriptionException if description is blank
     * @throws MalformedStatePartException if a part is malformed
     * @throws UnknownHealthStateLabelException if a state label is unknown
     * @throws DuplicateStateException if a state is listed more than once
     * @throws InvalidPercentageSumException if percentages do not sum to one
     * @throws IllegalArgumentException if epsilon is invalid
     */
    public static StateVector parse(
            final String description,
            final double epsilon
    )
            throws NullStateDescriptionException,
            EmptyStateDescriptionException,
            MalformedStatePartException,
            UnknownHealthStateLabelException,
            DuplicateStateException,
            InvalidPercentageSumException,
            IllegalArgumentException
    {
        if (description == null) {
            throw new NullStateDescriptionException();
        }
        Preconditions.checkArgument(
                Double.isFinite(epsilon) &&
                epsilon >= 0,
                "Epsilon must be finite and non-negative"
        );
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
            final HealthState state = HealthState.fromLabel(
                    matcher.group(2).toLowerCase(Locale.ROOT)
            );
            if (given[state.stateIndex()]) {
                throw new DuplicateStateException();
            }
            given[state.stateIndex()] = true;
            fractions[state.stateIndex()] = percentage / 100;
        }

        final Vector vector = new Vector(fractions);
        if (!StateVector.isDistribution(vector, epsilon)) {
            throw new InvalidPercentageSumException();
        }
        return new StateVector(vector, epsilon);
    }

}
