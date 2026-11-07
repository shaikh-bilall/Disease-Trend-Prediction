package diseaseTrendPrediction.io;

import domain.Diagonalization;
import domain.Matrix;
import com.google.common.base.Preconditions;
import diseaseTrendPrediction.io.exceptions.InvalidDiagonalizationFileException;
import diseaseTrendPrediction.io.exceptions.InvalidMatrixFileException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persists and retrieves matrices and diagonalizations. */
public final class MatrixStore {

    private static final String SEPARATOR = ",";
    private static final String LINE_END = "\n";
    private static final String TEMPORARY_SUFFIX = ".tmp";
    private static final String DIAGONALIZATION_MANIFEST_FORMAT = "disease-trend-prediction-diagonalization-manifest-v1";
    private static final String GENERATION_FILE_PREFIX = "diagonalization-";

    private record DiagonalizationFiles(Path p, Path d, Path inverseP) {}

    /** Saves a matrix. */
    public static void saveMatrix(
            final Path directory,
            final String fileName,
            final Matrix matrix
    )
            throws IOException
    {
        Preconditions.checkNotNull(matrix, "Matrix must not be null");

        writeMatrix(resolveFile(directory, fileName), matrix);
    }
    /** Loads a matrix. */
    public static Matrix loadMatrix(
            final Path directory,
            final String fileName
    )
            throws IOException
    {
        return readMatrix(resolveFile(directory, fileName));
    }
    /** Saves a diagonalization. */
    public static void saveDiagonalization(
            final Path directory,
            final String manifestFileName,
            final Diagonalization diagonalization
    )
            throws IOException
    {
        Preconditions.checkNotNull(diagonalization, "Diagonalization must not be null");
        final Path manifest = resolveFile(directory, manifestFileName);
        final DiagonalizationFiles files = createGenerationFiles(directory);
        final List<Path> writtenFiles = new ArrayList<>(3);
        try {
            writeMatrix(files.p(), diagonalization.P());
            writtenFiles.add(files.p());
            writeMatrix(files.d(), diagonalization.D());
            writtenFiles.add(files.d());
            writeMatrix(files.inverseP(), diagonalization.inverseP());
            writtenFiles.add(files.inverseP());
            writeAtomically(manifest, writer -> writeManifest(writer, files));
        } catch (IOException | RuntimeException e) {
            deleteFiles(writtenFiles, e);
            throw e;
        }
    }
    /** Loads a diagonalization. */
    public static Diagonalization loadDiagonalization(
            final Path directory,
            final String manifestFileName
    )
            throws IOException
    {
        final Path manifest = resolveFile(directory, manifestFileName);
        final DiagonalizationFiles files = readManifest(directory, manifest);
        final Matrix p = readDiagonalizationMatrix(files.p(), manifest, "P");
        final Matrix d = readDiagonalizationMatrix(files.d(), manifest, "D");
        final Matrix inverseP = readDiagonalizationMatrix(files.inverseP(), manifest, "INVERSE_P");
        try {
            return new Diagonalization(p, d, inverseP);
        } catch (IllegalStateException e) {
            throw invalidDiagonalization(manifest, e.getMessage(), e);
        }
    }

    // The path of a file in the directory, which must be a plain name.
    private static Path resolveFile(
            final Path directory,
            final String fileName
    ) {
        Preconditions.checkNotNull(directory, "Directory must not be null");
        Preconditions.checkNotNull(fileName, "File name must not be null");
        Preconditions.checkArgument(!fileName.isBlank(), "File name must not be blank");
        final Path name = Path.of(fileName);
        Preconditions.checkArgument(
                name.equals(name.getFileName()) && !fileName.equals(".") && !fileName.equals(".."),
                "File name must be a plain name without a folder part: %s",
                fileName
        );
        return directory.resolve(name);
    }
    // Writes one matrix to a file that is replaced atomically.
    private static void writeMatrix(
            final Path file,
            final Matrix matrix
    )
            throws IOException
    {
        writeAtomically(file, writer -> writeMatrixRows(writer, matrix));
    }
    // Writes the complete contents to a sibling temporary file before replacing the target in one move.
    private static void writeAtomically(
            final Path file,
            final WriterAction action
    )
            throws IOException
    {
        final Path absoluteFile = file.toAbsolutePath();
        final Path parent = absoluteFile.getParent();
        Files.createDirectories(parent);
        final Path temporary = Files.createTempFile(
                parent,
                absoluteFile.getFileName() + "...",
                TEMPORARY_SUFFIX
        );
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                action.write(writer);
            }
            Files.move(
                    temporary,
                    absoluteFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
    }
    // Creates separate matrix files for a new immutable generation.
    private static DiagonalizationFiles createGenerationFiles(
            final Path directory
    ) {
        final String generation = UUID.randomUUID().toString();
        return new DiagonalizationFiles(
                resolveFile(directory, GENERATION_FILE_PREFIX + generation + "-P.csv"),
                resolveFile(directory, GENERATION_FILE_PREFIX + generation + "-D.csv"),
                resolveFile(directory, GENERATION_FILE_PREFIX + generation + "-inverse-P.csv")
        );
    }
    // Publishes the filenames for a complete matrix generation.
    private static void writeManifest(
            final BufferedWriter writer,
            final DiagonalizationFiles files
    )
            throws IOException
    {
        writer.write(DIAGONALIZATION_MANIFEST_FORMAT);
        writer.write(LINE_END);
        writeManifestEntry(writer, "P", files.p());
        writeManifestEntry(writer, "D", files.d());
        writeManifestEntry(writer, "INVERSE_P", files.inverseP());
    }
    // Writes one matrix filename to the manifest.
    private static void writeManifestEntry(
            final BufferedWriter writer,
            final String section,
            final Path file
    )
            throws IOException
    {
        writer.write(section + SEPARATOR + file.getFileName());
        writer.write(LINE_END);
    }
    // Writes all matrix rows as comma-separated values.
    private static void writeMatrixRows(
            final BufferedWriter writer,
            final Matrix matrix
    )
            throws IOException
    {
        for (int i = 0; i < matrix.rows(); i++) {
            for (int j = 0; j < matrix.cols(); j++) {
                if (j > 0) {
                    writer.write(SEPARATOR);
                }
                writer.write(Double.toString(matrix.get(i, j)));
            }
            writer.write(LINE_END);
        }
    }
    // Reads and validates the generation published by a manifest.
    private static DiagonalizationFiles readManifest(
            final Path directory,
            final Path manifest
    )
            throws IOException
    {
        try (BufferedReader reader = Files.newBufferedReader(manifest, StandardCharsets.UTF_8)) {
            if (!DIAGONALIZATION_MANIFEST_FORMAT.equals(reader.readLine())) {
                throw invalidDiagonalization(manifest, "missing or unsupported manifest header", null);
            }
            final Path p = readManifestEntry(reader, directory, manifest, "P");
            final Path d = readManifestEntry(reader, directory, manifest, "D");
            final Path inverseP = readManifestEntry(reader, directory, manifest, "INVERSE_P");
            if (reader.readLine() != null) {
                throw invalidDiagonalization(manifest, "unexpected content after the matrix filenames", null);
            }
            if (p.equals(d) || p.equals(inverseP) || d.equals(inverseP)) {
                throw invalidDiagonalization(manifest, "matrix filenames must be different", null);
            }
            return new DiagonalizationFiles(p, d, inverseP);
        }
    }
    // Reads one matrix filename from a manifest.
    private static Path readManifestEntry(
            final BufferedReader reader,
            final Path directory,
            final Path manifest,
            final String section
    )
            throws IOException
    {
        final String entry = reader.readLine();
        final String prefix = section + SEPARATOR;
        if (entry == null || !entry.startsWith(prefix) || entry.indexOf(SEPARATOR, prefix.length()) >= 0) {
            throw invalidDiagonalization(manifest, "missing " + section + " matrix filename", null);
        }
        try {
            return resolveFile(directory, entry.substring(prefix.length()));
        } catch (IllegalArgumentException e) {
            throw invalidDiagonalization(manifest, "invalid filename for " + section, e);
        }
    }
    // Loads one matrix named by a manifest and reports invalid contents as a diagonalization failure.
    private static Matrix readDiagonalizationMatrix(
            final Path matrixFile,
            final Path manifest,
            final String section
    )
            throws IOException
    {
        try {
            return readMatrix(matrixFile);
        } catch (InvalidMatrixFileException e) {
            throw invalidDiagonalization(manifest, "invalid " + section + " matrix", e);
        }
    }
    // Creates a parameterless public error with details available through its cause.
    private static InvalidDiagonalizationFileException invalidDiagonalization(
            final Path file,
            final String detail,
            final Throwable cause
    ) {
        final InvalidDiagonalizationFileException exception = new InvalidDiagonalizationFileException();
        exception.initCause(new IllegalArgumentException("Invalid diagonalization in " + file + ": " + detail, cause));
        return exception;
    }
    // Deletes a partially written generation when the manifest could not be published.
    private static void deleteFiles(
            final List<Path> files,
            final Throwable failure
    ) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException | RuntimeException suppressed) {
                failure.addSuppressed(suppressed);
            }
        }
    }
    // Reads a matrix from the file, naming the file and line in the error if the content is invalid.
    private static Matrix readMatrix(
            final Path file
    )
            throws IOException
    {
        final List<double[]> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                rows.add(parseRow(line, file, rows.size() + 1));
            }
        }
        try {
            return new Matrix(rows.toArray(new double[0][]));
        } catch (IllegalStateException e) {
            final InvalidMatrixFileException exception = new InvalidMatrixFileException();
            exception.initCause(new IllegalArgumentException("Invalid matrix in " + file + ": " + e.getMessage(), e));
            throw exception;
        }
    }
    // The entries of one line.
    private static double[] parseRow(
            final String line,
            final Path file,
            final int lineNumber
    ) {
        final String[] cells = line.split(SEPARATOR, -1);
        final double[] row = new double[cells.length];
        for (int j = 0; j < cells.length; j++) {
            try {
                row[j] = Double.parseDouble(cells[j]);
            } catch (NumberFormatException e) {
                final InvalidMatrixFileException exception = new InvalidMatrixFileException();
                exception.initCause(new IllegalArgumentException(
                        "Invalid matrix in " + file + ": line " + lineNumber
                                + " has an entry that is not a number: '" + cells[j] + "'",
                        e
                ));
                throw exception;
            }
        }
        return row;
    }

    @FunctionalInterface
    private interface WriterAction {
        void write(BufferedWriter writer) throws IOException;
    }

}
