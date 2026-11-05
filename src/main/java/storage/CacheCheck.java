package storage;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * Decides whether stored results derived from a source file are still current, by comparing file timestamps. Results
 * are current only if they were stored after the source file was last changed.
 */
public final class CacheCheck {

    /**
     * Tells whether every cached file exists and is newer than the source file. A cached file with exactly the same
     * timestamp as the source counts as out of date.
     *
     * @param source      the file the cached files are derived from; it must exist
     * @param cachedFiles the files derived from the source, at least one
     * @return {@code true} if every cached file is a regular file modified after the source; {@code false} if any is
     *         missing or not newer
     * @throws NullPointerException     if {@code source}, {@code cachedFiles} or any cached file is {@code null}
     * @throws IllegalArgumentException if {@code cachedFiles} is empty
     * @throws IOException              if the source file is missing, or a timestamp cannot be read
     */
    public static boolean isUpToDate(
            final Path source,
            final Path... cachedFiles
    )
            throws IOException
    {
        Preconditions.checkNotNull(source, "Source must not be null");
        Preconditions.checkNotNull(cachedFiles, "Cached files must not be null");
        Preconditions.checkArgument(cachedFiles.length > 0, "At least one cached file is required");

        final FileTime sourceTime = Files.getLastModifiedTime(source);
        for (Path cachedFile : cachedFiles) {
            Preconditions.checkNotNull(cachedFile, "Cached file must not be null");
            if (!Files.isRegularFile(cachedFile) || Files.getLastModifiedTime(cachedFile).compareTo(sourceTime) <= 0) {
                return false;
            }
        }
        return true;
    }

}