package io;

import com.google.common.base.Preconditions;
import domain.HealthState;
import domain.PatientHistory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads the patient dataset CSV into patient histories. The file has the header {@code patient-id,time-step,state}
 * and one row per observation, in any order. The rows of each patient are grouped and put in time order, and every
 * patient must have the time steps 0, 1, 2, ... without gaps or repeats.
 */
public final class PatientCsvReader {

    private static final String HEADER = "patient-id,time-step,state";
    // Characters buffered per read of the file.
    private static final int BUFFER_SIZE = 1 << 16;
    // Rows the arrays hold at the start; they double when full.
    private static final int INITIAL_ROWS = 1 << 16;
    // Marks a place in the placed array that no row has filled yet.
    private static final byte NOT_PLACED = -1;

    // The rows as parallel arrays: the patient's group, the time step and the state ordinal of each row.
    private record Rows(int[] groups, int[] steps, byte[] states, int size) {}

    /**
     * Reads the patient histories from a CSV file.
     *
     * @param csvPath the file to read, encoded as UTF-8
     * @return the histories in ascending order of patient id; empty if the file has only the header
     * @throws NullPointerException     if {@code csvPath} is {@code null}
     * @throws IllegalArgumentException if the header is wrong, a row does not have three columns, an id or time step
     *                                  is not a non-negative integer, a state label is unknown, or a patient's time
     *                                  steps are not exactly 0, 1, 2, ...
     * @throws IOException              if the file cannot be read
     */
    public static List<PatientHistory> read(
            final Path csvPath
    )
            throws IOException
    {
        Preconditions.checkNotNull(csvPath, "CSV path must not be null");

        final PatientIdIndex index = new PatientIdIndex();
        final Rows rows = readRows(csvPath, index);
        final int[] offsets = getOffsets(index);
        final byte[] placed = placeStates(rows, index, offsets);
        return getHistories(index, offsets, placed);
    }

    // Parses every row into the arrays, numbering each patient's group as it first appears.
    private static Rows readRows(
            final Path csvPath,
            final PatientIdIndex index
    )
            throws IOException
    {
        int[] rowGroups = new int[INITIAL_ROWS];
        int[] rowSteps = new int[INITIAL_ROWS];
        byte[] rowStates = new byte[INITIAL_ROWS];
        int size = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(csvPath), StandardCharsets.UTF_8), BUFFER_SIZE)) {
            String line = reader.readLine();
            Preconditions.checkArgument(HEADER.equals(line), "The first line must be the header: %s", HEADER);

            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                final int firstComma = line.indexOf(',');
                final int secondComma = (firstComma < 0) ? -1 : line.indexOf(',', firstComma + 1);
                Preconditions.checkArgument(secondComma >= 0 && line.indexOf(',', secondComma + 1) < 0,
                        "Line %s must have exactly three columns", lineNumber);

                final int patientId = parseNonNegativeInt(line, 0, firstComma);
                Preconditions.checkArgument(patientId >= 0, "Line %s: patient-id must be a non-negative integer", lineNumber);
                final int timeStep = parseNonNegativeInt(line, firstComma + 1, secondComma);
                Preconditions.checkArgument(timeStep >= 0, "Line %s: time-step must be a non-negative integer", lineNumber);
                final HealthState state = parseState(line, secondComma + 1, lineNumber);

                if (size == rowGroups.length) {
                    rowGroups = Arrays.copyOf(rowGroups, size * 2);
                    rowSteps = Arrays.copyOf(rowSteps, size * 2);
                    rowStates = Arrays.copyOf(rowStates, size * 2);
                }
                rowGroups[size] = index.add(patientId);
                rowSteps[size] = timeStep;
                rowStates[size] = (byte) state.ordinal();
                size++;
            }
        }
        return new Rows(rowGroups, rowSteps, rowStates, size);
    }
    // The non-negative integer in line[from, to), or -1 if the range is empty, has a non-digit, or is too large.
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
    // The state whose label is the rest of the line from the given position, with the line number added to an error.
    private static HealthState parseState(
            final String line,
            final int from,
            final int lineNumber
    ) {
        try {
            return HealthState.fromLabel(line.substring(from));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Line " + lineNumber + ": " + e.getMessage(), e);
        }
    }
    // Where each patient's block starts in the placed array: offsets[g] to offsets[g + 1] belongs to group g.
    private static int[] getOffsets(
            final PatientIdIndex index
    ) {
        final int[] offsets = new int[index.groups() + 1];
        for (int group = 0; group < index.groups(); group++) {
            offsets[group + 1] = offsets[group] + index.rowCount(group);
        }
        return offsets;
    }
    // Puts every state at its time step within its patient's block, which sorts the block and exposes any gap or repeat.
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
            final boolean fits = step < offsets[group + 1] - start && placed[start + step] == NOT_PLACED;
            Preconditions.checkArgument(fits, "Patient %s: time steps must be 0, 1, 2, ... without gaps or repeats",
                    index.patientId(group));
            placed[start + step] = rowStates[row];
        }
        return placed;
    }
    // Builds the histories in ascending order of patient id.
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