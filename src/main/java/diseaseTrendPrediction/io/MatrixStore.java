package diseaseTrendPrediction.io;

import com.google.common.base.Preconditions;
import diseaseTrendPrediction.io.exceptions.AtomicMoveFailureException;
import diseaseTrendPrediction.io.exceptions.DuplicateMatrixFileNameException;
import diseaseTrendPrediction.io.exceptions.MatrixFileNotFoundException;
import diseaseTrendPrediction.io.exceptions.MatrixWriteFailureException;
import diseaseTrendPrediction.io.exceptions.NullMatrixStoreArgumentException;
import domain.Diagonalization;
import domain.Matrix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Stores and retrieves matrices and diagonalizations. */
public final class MatrixStore {

    private static final String SEPARATOR = ",";
    private static final String LINE_END = "\n";
    private static final String TEMPORARY_SUFFIX = ".tmp";
    private static final String DIAGONALIZATION_MANIFEST_FORMAT = "disease-trend-prediction-diagonalization-manifest-v1";
    private static final String GENERATION_FILE_PREFIX = "diagonalization-";

    private record DiagonalizationFiles(Path p, Path d, Path inverseP) {}

    private MatrixStore() {}

    /** Saves a matrix. */
    public static void saveMatrix(
            final Path directory,
            final String fileName,
            final Matrix matrix
    )
            throws MatrixWriteFailureException,
            AtomicMoveFailureException
    {
        checkNotNull(directory, fileName, matrix);
        writeMatrix(resolveFile(directory, fileName), matrix);
    }

    /** Loads a matrix. */
    public static Matrix loadMatrix(
            final Path directory,
            final String fileName
    )
            throws IOException
    {
        checkNotNull(directory, fileName);
        return readMatrix(resolveFile(directory, fileName));
    }

    /** Saves a diagonalization. */
    public static void saveDiagonalization(
            final Path directory,
            final String manifestFileName,
            final Diagonalization diagonalization
    )
            throws MatrixWriteFailureException,
            AtomicMoveFailureException
    {
        checkNotNull(directory, manifestFileName, diagonalization);
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
        } catch (
                MatrixWriteFailureException |
                AtomicMoveFailureException |
                RuntimeException e
        ) {
            deleteFiles(writtenFiles);
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
        checkNotNull(directory, manifestFileName);
        final Path manifest = resolveFile(directory, manifestFileName);
        final DiagonalizationFiles files = readManifest(directory, manifest);
        checkDistinctFiles(files);
        final Matrix p = readMatrix(files.p());
        final Matrix d = readMatrix(files.d());
        final Matrix inverseP = readMatrix(files.inverseP());

        return new Diagonalization(p, d, inverseP);
    }

    private static void checkNotNull(
            final Object... arguments
    ) {
        for (Object argument : arguments) {
            if (argument == null) {
                throw new NullMatrixStoreArgumentException();
            }
        }
    }

    // Resolves a plain filename inside the given directory.
    private static Path resolveFile(
            final Path directory,
            final String fileName
    ) {
        Preconditions.checkArgument(!fileName.isBlank(), "File name must not be blank");
        final Path name = Path.of(fileName);
        Preconditions.checkArgument(
                name.equals(name.getFileName()) &&
                !fileName.equals(".") &&
                !fileName.equals(".."),
                "File name must be a plain name without a folder part: %s",
                fileName
        );
        return directory.resolve(name);
    }

    private static void writeMatrix(
            final Path file,
            final Matrix matrix
    )
            throws MatrixWriteFailureException, AtomicMoveFailureException
    {
        writeAtomically(file, writer -> writeMatrixRows(writer, matrix));
    }

    private static void writeAtomically(
            final Path file,
            final WriterAction action
    )
            throws MatrixWriteFailureException, AtomicMoveFailureException
    {
        final Path absoluteFile = file.toAbsolutePath();
        final Path parent = absoluteFile.getParent();
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(
                    parent,
                    absoluteFile.getFileName() + "...",
                    TEMPORARY_SUFFIX
            );
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                action.write(writer);
            }
        } catch (IOException e) {
            if (temporary != null) {
                deleteFiles(List.of(temporary));
            }
            throw new MatrixWriteFailureException();
        } catch (RuntimeException e) {
            if (temporary != null) {
                deleteFiles(List.of(temporary));
            }
            throw e;
        }

        try {
            Files.move(
                    temporary,
                    absoluteFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            deleteFiles(List.of(temporary));
            throw new AtomicMoveFailureException();
        }
    }

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

    private static void writeMatrixRows(
            final BufferedWriter writer,
            final Matrix matrix
    )
            throws IOException
    {
        for (int row = 0; row < matrix.rows(); row++) {
            for (int column = 0; column < matrix.cols(); column++) {
                if (column > 0) {
                    writer.write(SEPARATOR);
                }
                writer.write(Double.toString(matrix.get(row, column)));
            }
            writer.write(LINE_END);
        }
    }

    private static DiagonalizationFiles readManifest(
            final Path directory,
            final Path manifest
    )
            throws IOException
    {
        try (BufferedReader reader = openReader(manifest)) {
            Preconditions.checkArgument(DIAGONALIZATION_MANIFEST_FORMAT.equals(reader.readLine()));
            final Path p = readManifestEntry(reader, directory, "P");
            final Path d = readManifestEntry(reader, directory, "D");
            final Path inverseP = readManifestEntry(reader, directory, "INVERSE_P");
            Preconditions.checkArgument(reader.readLine() == null);
            return new DiagonalizationFiles(p, d, inverseP);
        }
    }

    private static Path readManifestEntry(
            final BufferedReader reader,
            final Path directory,
            final String section
    )
            throws IOException
    {
        final String entry = reader.readLine();
        final String prefix = section + SEPARATOR;
        Preconditions.checkArgument(
                entry != null && entry.startsWith(prefix) && entry.indexOf(SEPARATOR, prefix.length()) < 0
        );
        return resolveFile(directory, entry.substring(prefix.length()));
    }

    private static void checkDistinctFiles(
            final DiagonalizationFiles files
    ) {
        if (files.p().equals(files.d()) || files.p().equals(files.inverseP()) || files.d().equals(files.inverseP())) {
            throw new DuplicateMatrixFileNameException();
        }
    }

    private static Matrix readMatrix(
            final Path file
    )
            throws IOException
    {
        final List<double[]> rows = new ArrayList<>();
        try (BufferedReader reader = openReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                rows.add(parseRow(line));
            }
        }
        return new Matrix(rows.toArray(new double[0][]));
    }

    private static BufferedReader openReader(
            final Path file
    )
            throws IOException
    {
        try {
            return Files.newBufferedReader(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw new MatrixFileNotFoundException();
        }
    }

    private static double[] parseRow(
            final String line
    ) {
        final String[] cells = line.split(SEPARATOR, -1);
        final double[] row = new double[cells.length];
        for (int column = 0; column < cells.length; column++) {
            row[column] = Double.parseDouble(cells[column]);
        }
        return row;
    }

    private static void deleteFiles(
            final List<Path> files
    ) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // Cleanup is best-effort after a failed write.
            }
        }
    }

    @FunctionalInterface
    private interface WriterAction {
        void write(BufferedWriter writer) throws IOException;
    }

}
