package io;

import domain.Diagonalization;
import domain.Matrix;
import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves and loads matrices as CSV files: one matrix per file, one row per line, entries separated by commas, and no
 * header. A diagonalization is kept in three files, for {@code P}, {@code D} and the inverse of {@code P}. Numbers are
 * written in the shortest form that reads back as the very same value, so a loaded matrix equals the saved one. Every
 * write goes through a temporary file that is then moved into place, so a crash never leaves a truncated file.
 */
public final class MatrixStore {

    private static final String SEPARATOR = ",";
    private static final String LINE_END = "\n";
    private static final String TEMPORARY_SUFFIX = ".tmp";

    // The three files of a diagonalization.
    private record DiagonalizationFiles(Path p, Path d, Path inverseP) {}

    /**
     * Saves a matrix to a file, replacing the file if it exists.
     *
     * @param directory the folder of the file, created if it does not exist
     * @param fileName  the plain name of the file, without any folder part
     * @param matrix    the matrix to save
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code fileName} is blank or has a folder part
     * @throws IOException              if the file cannot be written
     */
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
    /**
     * Loads a matrix from a file written by {@link #saveMatrix}.
     *
     * @param directory the folder of the file
     * @param fileName  the plain name of the file, without any folder part
     * @return the matrix in the file
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code fileName} is blank or has a folder part, or the content is not a
     *                                  valid matrix: no rows, an entry that is not a finite number, or rows of
     *                                  different lengths
     * @throws IOException              if the file is missing or cannot be read
     */
    public static Matrix loadMatrix(
            final Path directory,
            final String fileName
    )
            throws IOException
    {
        return readMatrix(resolveFile(directory, fileName));
    }
    /**
     * Saves the three matrices of a diagonalization to three files, replacing any that exist.
     *
     * @param directory        the folder of the files, created if it does not exist
     * @param pFileName        the plain name of the file for {@code P}
     * @param dFileName        the plain name of the file for {@code D}
     * @param inversePFileName the plain name of the file for the inverse of {@code P}
     * @param diagonalization  the diagonalization to save
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if a file name is blank, has a folder part, or equals another file name
     * @throws IOException              if a file cannot be written
     */
    public static void saveDiagonalization(
            final Path directory,
            final String pFileName,
            final String dFileName,
            final String inversePFileName,
            final Diagonalization diagonalization
    )
            throws IOException
    {
        Preconditions.checkNotNull(diagonalization, "Diagonalization must not be null");
        final DiagonalizationFiles files = resolveFiles(directory, pFileName, dFileName, inversePFileName);

        writeMatrix(files.p(), diagonalization.P());
        writeMatrix(files.d(), diagonalization.D());
        writeMatrix(files.inverseP(), diagonalization.inverseP());
    }
    /**
     * Loads a diagonalization from three files written by {@link #saveDiagonalization}.
     *
     * @param directory        the folder of the files
     * @param pFileName        the plain name of the file for {@code P}
     * @param dFileName        the plain name of the file for {@code D}
     * @param inversePFileName the plain name of the file for the inverse of {@code P}
     * @return the diagonalization in the files
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if a file name is blank, has a folder part, or equals another file name, a
     *                                  file does not hold a valid matrix, or the three matrices do not form a
     *                                  valid diagonalization
     * @throws IOException              if a file is missing or cannot be read
     */
    public static Diagonalization loadDiagonalization(
            final Path directory,
            final String pFileName,
            final String dFileName,
            final String inversePFileName
    )
            throws IOException
    {
        final DiagonalizationFiles files = resolveFiles(directory, pFileName, dFileName, inversePFileName);

        final Matrix p = readMatrix(files.p());
        final Matrix d = readMatrix(files.d());
        final Matrix inverseP = readMatrix(files.inverseP());
        try {
            return new Diagonalization(p, d, inverseP);

        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(
                    "Invalid diagonalization in " + files.p() + ", " + files.d() + " and " + files.inverseP() + ": " + e.getMessage(), e);
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
        Preconditions.checkArgument(name.equals(name.getFileName()) && !fileName.equals(".") && !fileName.equals(".."),
                "File name must be a plain name without a folder part: %s", fileName);
        return directory.resolve(name);
    }
    // The paths of the three diagonalization files, which must be three different files.
    private static DiagonalizationFiles resolveFiles(
            final Path directory,
            final String pFileName,
            final String dFileName,
            final String inversePFileName
    ) {
        final DiagonalizationFiles files = new DiagonalizationFiles(
                resolveFile(directory, pFileName),
                resolveFile(directory, dFileName),
                resolveFile(directory, inversePFileName)
        );
        Preconditions.checkArgument(!files.p().equals(files.d()) && !files.p().equals(files.inverseP())
                && !files.d().equals(files.inverseP()), "The three file names must be different");
        return files;
    }
    // Writes the matrix to a temporary file next to the target, then moves it over the target in one step.
    private static void writeMatrix(
            final Path file,
            final Matrix matrix
    )
            throws IOException
    {
        final Path temporary = file.resolveSibling(file.getFileName() + TEMPORARY_SUFFIX);
        Files.createDirectories(temporary.toAbsolutePath().getParent());
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
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
            // An atomic move replaces an existing target on its own, so no other option is needed.
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
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
            throw new IllegalArgumentException("Invalid matrix in " + file + ": " + e.getMessage(), e);
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
                throw new IllegalArgumentException(
                        "Invalid matrix in " + file + ": line " + lineNumber + " has an entry that is not a number: '" + cells[j] + "'", e);
            }
        }
        return row;
    }

}