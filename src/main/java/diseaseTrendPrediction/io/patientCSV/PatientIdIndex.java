package diseaseTrendPrediction.io.patientCSV;

import com.google.common.base.Preconditions;

import java.util.Arrays;

/** Tracks patient identifiers and their observation counts. */
final class PatientIdIndex {

    // Table slots at the start: a power of two, with at most half of them in use.
    private static final int INITIAL_SLOTS = 1 << 16;
    // Marks a table slot that holds no patient.
    private static final int EMPTY = -1;

    private int[] slotIds = new int[INITIAL_SLOTS];
    private int[] slotGroups = newEmptySlots(INITIAL_SLOTS);
    private int[] groupIds = new int[INITIAL_SLOTS / 2];
    private int[] groupRowCounts = new int[INITIAL_SLOTS / 2];
    private int groups;

    /** Records an observation for a patient. */
    public int add(
            final int patientId
    ) {
        Preconditions.checkArgument(patientId >= 0, "Patient id must be non-negative");
        final int mask = slotIds.length - 1;
        int slot = hash(patientId) & mask;
        while (slotGroups[slot] != EMPTY && slotIds[slot] != patientId) {
            slot = (slot + 1) & mask;
        }
        int group = slotGroups[slot];
        if (group == EMPTY) {
            group = addGroup(patientId, slot);
        }
        groupRowCounts[group]++;
        return group;
    }
    /** Returns the number of patients tracked. */
    public int groups() {
        return groups;
    }
    /** Returns a patient identifier. */
    public int patientId(
            final int group
    ) {
        Preconditions.checkElementIndex(group, groups, "Group");
        return groupIds[group];
    }
    /** Returns the number of observations for a patient. */
    public int rowCount(
            final int group
    ) {
        Preconditions.checkElementIndex(group, groups, "Group");
        return groupRowCounts[group];
    }
    // Registers a new patient in the given empty slot, growing the arrays when they fill up.
    private int addGroup(
            final int patientId,
            final int slot
    ) {
        if (groups == groupIds.length) {
            groupIds = Arrays.copyOf(groupIds, groups * 2);
            groupRowCounts = Arrays.copyOf(groupRowCounts, groups * 2);
        }
        final int group = groups++;
        groupIds[group] = patientId;
        slotIds[slot] = patientId;
        slotGroups[slot] = group;
        if (groups * 2 > slotIds.length) {
            rehash();
        }
        return group;
    }
    // Doubles the table and puts every patient back in.
    private void rehash() {
        final int slots = slotIds.length * 2;
        final int mask = slots - 1;
        final int[] newSlotIds = new int[slots];
        final int[] newSlotGroups = newEmptySlots(slots);
        for (int group = 0; group < groups; group++) {
            int slot = hash(groupIds[group]) & mask;
            while (newSlotGroups[slot] != EMPTY) {
                slot = (slot + 1) & mask;
            }
            newSlotIds[slot] = groupIds[group];
            newSlotGroups[slot] = group;
        }
        slotIds = newSlotIds;
        slotGroups = newSlotGroups;
    }
    // Spreads the bits of the id so that neighboring ids do not cluster in the table.
    private static int hash(
            final int patientId
    ) {
        final int h = patientId * 0x9E3779B9;
        return h ^ (h >>> 16);
    }
    // Returns a group array of the given size with every slot empty.
    private static int[] newEmptySlots(
            final int slots
    ) {
        final int[] empty = new int[slots];
        Arrays.fill(empty, EMPTY);
        return empty;
    }

}
