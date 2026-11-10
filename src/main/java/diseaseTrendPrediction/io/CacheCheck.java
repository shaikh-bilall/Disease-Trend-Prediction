package diseaseTrendPrediction.io;

import com.google.common.base.Preconditions;

import java.time.Instant;

/** Compares source and cached-data timestamps. */
public final class CacheCheck {

    // Prevents instances of this stateless utility.
    private CacheCheck() {}

    /**
     * Returns whether a cached snapshot was built from the same source version.
     *
     * @param sourceTimestamp current source timestamp
     * @param snapshotTimestamp timestamp stored with the snapshot
     * @return whether both timestamps identify the same source version
     */
    public static boolean isUpToDate(
            final Instant sourceTimestamp,
            final Instant snapshotTimestamp
    ) {
        Preconditions.checkNotNull(
            sourceTimestamp,
            "Source timestamp must not be null"
        );
        Preconditions.checkNotNull(
            snapshotTimestamp,
            "Snapshot timestamp must not be null"
        );
        return snapshotTimestamp.equals(sourceTimestamp);
    }
}
