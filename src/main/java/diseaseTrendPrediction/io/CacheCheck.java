package diseaseTrendPrediction.io;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/** Checks whether cached results remain current relative to their source. */
public final class CacheCheck {

    /** Checks whether cached results are current for a source file. */
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
        boolean allCachedFilesExistAndAreNew = true;
        int i = 0;
        while (allCachedFilesExistAndAreNew && i < cachedFiles.length) {
            final Path cachedFile = cachedFiles[i];
            Preconditions.checkNotNull(cachedFile, "Cached file must not be null");
            if (
                    !Files.isRegularFile(cachedFile) ||
                    Files.getLastModifiedTime(cachedFile).compareTo(sourceTime) <= 0
            ) {
                allCachedFilesExistAndAreNew = false;
            }
            i++;
        }
        return allCachedFilesExistAndAreNew;
    }

}
