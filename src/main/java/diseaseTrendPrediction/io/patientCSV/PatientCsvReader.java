package diseaseTrendPrediction.io.patientCSV;

import com.google.common.base.Preconditions;
import diseaseTrendPrediction.domain.HealthState;
import diseaseTrendPrediction.domain.PatientHistory;
import diseaseTrendPrediction.domain.exceptions.UnknownHealthStateLabelException;
import diseaseTrendPrediction.io.exceptions.InvalidPatientCsvHeaderException;
import diseaseTrendPrediction.io.exceptions.InvalidPatientCsvStateException;
import diseaseTrendPrediction.io.exceptions.InvalidPatientIdException;
import diseaseTrendPrediction.io.exceptions.InvalidPatientTimeSequenceException;
import diseaseTrendPrediction.io.exceptions.InvalidPatientTimeStepException;
import diseaseTrendPrediction.io.exceptions.MalformedPatientCsvRowException;
import diseaseTrendPrediction.io.exceptions.PatientCsvReadException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Reads patient observations from CSV files. */
public final class PatientCsvReader {

    private static final String HEADER = "patient-id,time-step,state";
    private static final int BUFFER_SIZE = 1 << 16;
    private static final int INITIAL_ROWS = 1 << 16;
    private static final byte NOT_PLACED = -1;

    private record Rows(
            int[] groups,
            int[] steps,
            byte[] states,
            int size
    ) {

        // Creates a row collection with validated component values.
        private Rows(
                final int[] groups,
                final int[] steps,
                final byte[] states,
                final int size
        ) {
            this.groups = groups;
            this.steps = steps;
            this.states = states;
            this.size = size;
            checkRows();
        }

        // Checks that the row arrays can represent the declared row count.
        private void checkRows() {
            Preconditions.checkNotNull(
                groups,
                "Row groups must not be null"
            );
            Preconditions.checkNotNull(
                steps,
                "Row steps must not be null"
            );
            Preconditions.checkNotNull(
                states,
                "Row states must not be null"
            );
            Preconditions.checkState(
                size >= 0 &&
                size <= groups.length &&
                size <= steps.length &&
                size <= states.length,
                "Row count must fit every row array"
            );
        }
    }

    /**
     * Reads patient histories from a CSV file.
     *
     * @param csvPath source CSV path
     * @return histories read from the CSV
     * @throws PatientCsvReadException if the CSV cannot be read
     * @throws InvalidPatientCsvHeaderException if the header is invalid
     * @throws MalformedPatientCsvRowException if a row is malformed
     * @throws InvalidPatientIdException if a patient identifier is invalid
     * @throws InvalidPatientTimeStepException if a time step is invalid
     * @throws InvalidPatientCsvStateException if a state label is unknown
     * @throws InvalidPatientTimeSequenceException if patient time steps are invalid
     */
    public static List<PatientHistory> read(
            final Path csvPath
    )
            throws PatientCsvReadException,
            InvalidPatientCsvHeaderException,
            MalformedPatientCsvRowException,
            InvalidPatientIdException,
            InvalidPatientTimeStepException,
            InvalidPatientCsvStateException,
            InvalidPatientTimeSequenceException
    {
        Preconditions.checkNotNull(
            csvPath,
            "CSV path must not be null"
        );

        final PatientIdIndex index = new PatientIdIndex();
        final Rows rows = readRows(csvPath, index);
        final int[] offsets = getOffsets(index);
        final byte[] placed = placeStates(rows, index, offsets);
        return getHistories(index, offsets, placed);
    }

    // Reads and validates patient observation rows.
    private static Rows readRows(
            final Path csvPath,
            final PatientIdIndex index
    )
            throws PatientCsvReadException
    {
        int[] rowGroups = new int[INITIAL_ROWS];
        int[] rowSteps = new int[INITIAL_ROWS];
        byte[] rowStates = new byte[INITIAL_ROWS];
        int size = 0;

        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        Files.newInputStream(csvPath),
                        StandardCharsets.UTF_8
                ),
                BUFFER_SIZE
            )
        ) {
            String line = reader.readLine();
            if (!HEADER.equals(line)) {
                throw new InvalidPatientCsvHeaderException();
            }

            while ((line = reader.readLine()) != null) {
                final int firstComma = line.indexOf(',');
                final int secondComma = (firstComma < 0) ? -1 : line.indexOf(',', firstComma + 1);
                if (secondComma < 0 || line.indexOf(',', secondComma + 1) >= 0) {
                    throw new MalformedPatientCsvRowException();
                }

                final int patientId = parseNonNegativeInt(line, 0, firstComma);
                if (patientId < 0) {
                    throw new InvalidPatientIdException();
                }
                final int timeStep = parseNonNegativeInt(line, firstComma + 1, secondComma);
                if (timeStep < 0) {
                    throw new InvalidPatientTimeStepException();
                }
                final HealthState state = parseState(line, secondComma + 1);

                if (size == rowGroups.length) {
                    rowGroups = Arrays.copyOf(rowGroups, size * 2);
                    rowSteps = Arrays.copyOf(rowSteps, size * 2);
                    rowStates = Arrays.copyOf(rowStates, size * 2);
                }
                rowGroups[size] = index.add(patientId);
                rowSteps[size] = timeStep;
                rowStates[size] = (byte) state.stateIndex();
                size++;
            }
        } catch (IOException e) {
            throw new PatientCsvReadException();
        }
        return new Rows(rowGroups, rowSteps, rowStates, size);
    }
    // Parses a non-negative integer from a row segment.
    private static int parseNonNegativeInt(
            final String line,
            final int from,
            final int to
    ) {
        if (from >= to) {
            return -1;
        }
        long value = 0;
        for (int i = from; i < to; i++) {
            final int digit = line.charAt(i) - '0';
            if (digit < 0 || digit > 9) {
                return -1;
            }
            value = value * 10 + digit;
            if (value > Integer.MAX_VALUE) {
                return -1;
            }
        }
        return (int) value;
    }
    // Parses the health-state label in a row.
    private static HealthState parseState(
            final String line,
            final int from
    ) {
        try {
            return HealthState.fromLabel(line.substring(from));
        } catch (UnknownHealthStateLabelException e) {
            throw new InvalidPatientCsvStateException();
        }
    }
    // Builds patient history offsets from observation counts.
    private static int[] getOffsets(
            final PatientIdIndex index
    ) {
        final int[] offsets = new int[index.groups() + 1];
        for (int group = 0; group < index.groups(); group++) {
            offsets[group + 1] = offsets[group] + index.rowCount(group);
        }
        return offsets;
    }
    // Places each observation at its patient's time-step index.
    private static byte[] placeStates(
            final Rows rows,
            final PatientIdIndex index,
            final int[] offsets
    ) {
        final int[] rowGroups = rows.groups();
        final int[] rowSteps = rows.steps();
        final byte[] rowStates = rows.states();
        final byte[] placed = new byte[rows.size()];
        Arrays.fill(placed, NOT_PLACED);

        for (int row = 0; row < rows.size(); row++) {
            final int group = rowGroups[row];
            final int start = offsets[group];
            final int step = rowSteps[row];
            final boolean fits = step < (offsets[group + 1] - start) &&
                                 placed[start + step] == NOT_PLACED;
            if (!fits) {
                throw new InvalidPatientTimeSequenceException();
            }
            placed[start + step] = rowStates[row];
        }
        return placed;
    }
    // Builds patient histories ordered by patient identifier.
    private static List<PatientHistory> getHistories(
            final PatientIdIndex index,
            final int[] offsets,
            final byte[] placed
    ) {
        final long[] order = new long[index.groups()];
        for (int group = 0; group < order.length; group++) {
            order[group] = ((long) index.patientId(group) << 32) | group;
        }
        Arrays.sort(order);

        final HealthState[] allStates = HealthState.values();
        final List<PatientHistory> histories = new ArrayList<>(order.length);
        for (long entry : order) {
            final int group = (int) entry;
            final HealthState[] states = new HealthState[offsets[group + 1] - offsets[group]];
            for (int i = 0; i < states.length; i++) {
                states[i] = allStates[placed[offsets[group] + i]];
            }
            histories.add(new PatientHistory(index.patientId(group), List.of(states)));
        }
        return histories;
    }

}
