package diseaseTrendPrediction.io.patientCSV;

import com.google.common.base.Preconditions;

import java.util.Arrays;

/** Tracks patient identifiers and their observation counts. */
final class PatientIdIndex {

    private static final int INITIAL_SLOTS = 1 << 16;
    private static final int EMPTY = -1;

    private int[] slotIds;
    private int[] slotGroups;
    private int[] groupIds;
    private int[] groupRowCounts;
    private int groups;

    /**
     * Creates an empty patient index.
     *
     * @throws IllegalStateException if the index invariants are not satisfied
     */
    public PatientIdIndex() {
        this.slotIds = new int[INITIAL_SLOTS];
        this.slotGroups = newEmptySlots(INITIAL_SLOTS);
        this.groupIds = new int[INITIAL_SLOTS / 2];
        this.groupRowCounts = new int[INITIAL_SLOTS / 2];
        this.groups = 0;
        checkPatientIdIndex();
    }

    /**
     * Records an observation for a patient.
     *
     * @param patientId patient identifier
     * @return index of the patient's group
     * @throws IllegalArgumentException if the identifier is negative
     */
    public int add(
            final int patientId
    )
            throws IllegalArgumentException
    {
        Preconditions.checkArgument(
                patientId >= 0,
                "Patient id must be non-negative"
        );
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
        checkPatientIdIndex();
        return group;
    }
    /**
     * Returns the number of patients tracked.
     *
     * @return number of tracked patients
     */
    public int groups() {
        return groups;
    }
    /**
     * Returns a patient identifier.
     *
     * @param group patient group index
     * @return patient identifier
     * @throws IndexOutOfBoundsException if group is outside the tracked range
     */
    public int patientId(
            final int group
    )
            throws IndexOutOfBoundsException
    {
        Preconditions.checkElementIndex(
                group,
                groups,
                "Group"
        );
        return groupIds[group];
    }
    /**
     * Returns the number of observations for a patient.
     *
     * @param group patient group index
     * @return number of observations
     * @throws IndexOutOfBoundsException if group is outside the tracked range
     */
    public int rowCount(
            final int group
    )
            throws IndexOutOfBoundsException
    {
        Preconditions.checkElementIndex(
                group,
                groups,
                "Group"
        );
        return groupRowCounts[group];
    }
    // Checks the index's internal array and capacity invariants.
    private void checkPatientIdIndex() {
        Preconditions.checkState(
                slotIds.length == slotGroups.length,
                "Patient id and group tables must have equal lengths"
        );
        Preconditions.checkState(
                slotIds.length > 0 &&
                (slotIds.length & (slotIds.length - 1)) == 0,
                "Patient table size must be a positive power of two"
        );
        Preconditions.checkState(
                groups >= 0 &&
                groups <= groupIds.length,
                "Patient group count must fit its id table"
        );
        Preconditions.checkState(
                groups <= groupRowCounts.length,
                "Patient group count must fit its row-count table"
        );
        Preconditions.checkState(
                groups * 2 <= slotIds.length,
                "Patient table must remain at most half full"
        );
    }
    // Registers a new patient in the index.
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
        checkPatientIdIndex();
        return group;
    }
    // Resizes the lookup table.
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
        checkPatientIdIndex();
    }
    // Hashes a patient identifier.
    private static int hash(
            final int patientId
    ) {
        final int h = patientId * 0x9E3779B9;
        return h ^ (h >>> 16);
    }
    // Creates a lookup table with every slot empty.
    private static int[] newEmptySlots(
            final int slots
    ) {
        final int[] empty = new int[slots];
        Arrays.fill(empty, EMPTY);
        return empty;
    }

}
