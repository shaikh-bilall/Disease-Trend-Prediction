package diseaseTrendPrediction.io.transitionDataStorage;

import com.google.common.base.Preconditions;
import diseaseTrendPrediction.io.exceptions.TransitionDataIntegrityException;
import diseaseTrendPrediction.io.exceptions.TransitionDataNotFoundException;
import diseaseTrendPrediction.io.exceptions.TransitionDataPersistenceException;
import diseaseTrendPrediction.io.exceptions.TransitionDataReadException;
import linearAlgebra.domain.Diagonalization;
import linearAlgebra.domain.Matrix;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/** Persists transition matrices and their source freshness metadata. */
public final class MatrixStore {

    private final Path databasePath;

    /**
     * Creates a matrix store for the supplied database file.
     *
     * @param databasePath SQLite database file
     */
    public MatrixStore(
            final Path databasePath
    ) {
        Preconditions.checkNotNull(
            databasePath,
            "Database path must not be null"
        );
        this.databasePath = databasePath;
        checkMatrixStore();
    }

    /**
     * Saves all transition data as one atomic snapshot.
     *
     * @param transition transition matrix
     * @param diagonalization transition matrix diagonalization
     * @param sourceTimestamp timestamp of the source data used to build the matrices
     * @throws TransitionDataPersistenceException if the snapshot cannot be committed
     */
    public void saveTransitionData(
            final Matrix transition,
            final Diagonalization diagonalization,
            final Instant sourceTimestamp
    )
            throws TransitionDataPersistenceException
    {
        checkMatrixStore();
        Preconditions.checkNotNull(
            transition,
            "Transition matrix must not be null"
        );
        Preconditions.checkNotNull(
            diagonalization,
            "Diagonalization must not be null"
        );
        Preconditions.checkNotNull(
            sourceTimestamp,
            "Source timestamp must not be null"
        );

        final Matrix p = diagonalization.P();
        final Matrix d = diagonalization.D();
        final Matrix inverseP = diagonalization.inverseP();
        checkDimensions(transition, p, d, inverseP);

        try (
            Connection connection = Database.open(databasePath)
        ) {
            connection.setAutoCommit(false);
            try {
                clearSnapshot(connection);
                saveMatrix(connection, "transition_matrix", transition);
                saveMatrix(connection, "p_matrix", p);
                saveMatrix(connection, "d_matrix", d);
                saveMatrix(connection, "inverse_p_matrix", inverseP);
                try (
                    PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO transition_snapshot(state_count, source_timestamp) " +
                            "VALUES (?, ?)"
                    )
                ) {
                    statement.setInt(1, transition.rows());
                    statement.setString(2, sourceTimestamp.toString());
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                if (e instanceof SQLException) {
                    throw new TransitionDataPersistenceException();
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new TransitionDataPersistenceException();
        }
    }

    /**
     * Loads the complete persisted transition snapshot.
     *
     * @return persisted matrices and source timestamp
     * @throws TransitionDataNotFoundException if no snapshot exists
     * @throws TransitionDataIntegrityException if the snapshot is incomplete or invalid
     * @throws TransitionDataReadException if a database operation fails
     */
    public TransitionData loadTransitionData()
            throws TransitionDataNotFoundException,
            TransitionDataIntegrityException,
            TransitionDataReadException
    {
        try (
            Connection connection = Database.open(databasePath)
        ) {
            connection.setAutoCommit(false);
            final int stateCount;
            final Instant timestamp;
            try (
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                    "SELECT state_count, source_timestamp " +
                        "FROM transition_snapshot " +
                        "WHERE id = 1"
                )
            ) {
                if (!result.next()) {
                    throw new TransitionDataNotFoundException();
                }
                stateCount = result.getInt("state_count");
                if (stateCount <= 0) {
                    throw new TransitionDataIntegrityException();
                }
                timestamp = parseSourceTimestamp(result.getString("source_timestamp"));
            }

            final Matrix transition = loadMatrix(connection, "transition_matrix", stateCount);
            final Matrix p = loadMatrix(connection, "p_matrix", stateCount);
            final Matrix d = loadMatrix(connection, "d_matrix", stateCount);
            final Matrix inverseP = loadMatrix(connection, "inverse_p_matrix", stateCount);
            final TransitionData result = new TransitionData(
                transition,
                new Diagonalization(p, d, inverseP),
                timestamp
            );
            connection.commit();
            return result;
        } catch (TransitionDataNotFoundException e) {
            throw e;
        } catch (TransitionDataIntegrityException e) {
            throw e;
        } catch (TransitionDataReadException e) {
            throw e;
        } catch (SQLException e) {
            throw new TransitionDataReadException();
        }
    }

    // Checks that this store has a usable database path.
    private void checkMatrixStore() {
        Preconditions.checkNotNull(
            databasePath,
            "Database path must not be null"
        );
    }

    // Checks that the snapshot matrices have one common square shape.
    private static void checkDimensions(
            final Matrix... matrices
    ) {
        final int size = matrices[0].rows();
        Preconditions.checkArgument(
            size > 0,
            "Transition matrices must not be empty"
        );
        for (Matrix matrix : matrices) {
            Preconditions.checkArgument(
                matrix.rows() == size &&
                matrix.cols() == size,
                "All transition matrices must be square and have equal dimensions"
            );
        }
    }

    // Deletes the prior snapshot inside the active transaction.
    private static void clearSnapshot(
            final Connection connection
    )
            throws TransitionDataPersistenceException
    {
        try (
            Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate(
                "DELETE FROM transition_snapshot"
            );
            statement.executeUpdate(
                "DELETE FROM transition_matrix"
            );
            statement.executeUpdate(
                "DELETE FROM p_matrix"
            );
            statement.executeUpdate(
                "DELETE FROM d_matrix"
            );
            statement.executeUpdate(
                "DELETE FROM inverse_p_matrix"
            );
        } catch (SQLException e) {
            throw new TransitionDataPersistenceException();
        }
    }

    // Inserts every cell in one matrix table.
    private static void saveMatrix(
            final Connection connection,
            final String table,
            final Matrix matrix
    )
            throws TransitionDataPersistenceException
    {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + table + "(row_index, column_index, value) " +
                    "VALUES (?, ?, ?)"
            )
        ) {
            for (int row = 0; row < matrix.rows(); row++) {
                for (int column = 0; column < matrix.cols(); column++) {
                    statement.setInt(1, row);
                    statement.setInt(2, column);
                    statement.setDouble(3, matrix.get(row, column));
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new TransitionDataPersistenceException();
        }
    }

    // Loads and validates one matrix from its cell table.
    private static Matrix loadMatrix(
            final Connection connection,
            final String table,
            final int size
    )
            throws TransitionDataIntegrityException,
            TransitionDataReadException
    {
        final double[][] entries = new double[size][size];
        final boolean[][] found = new boolean[size][size];
        try (
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(
                "SELECT row_index, column_index, value FROM " + table
            )
        ) {
            while (result.next()) {
                final int row = result.getInt("row_index");
                final int column = result.getInt("column_index");
                if (
                    row < 0 ||
                    row >= size ||
                    column < 0 ||
                    column >= size ||
                    found[row][column]
                ) {
                    throw new TransitionDataIntegrityException();
                }
                entries[row][column] = result.getDouble("value");
                found[row][column] = true;
            }
        } catch (TransitionDataIntegrityException e) {
            throw e;
        } catch (SQLException e) {
            throw new TransitionDataReadException();
        }
        for (boolean[] row : found) {
            for (boolean cell : row) {
                if (!cell) {
                    throw new TransitionDataIntegrityException();
                }
            }
        }
        return new Matrix(entries);
    }

    // Parses a persisted source timestamp.
    private static Instant parseSourceTimestamp(
            final String sourceTimestamp
    )
            throws TransitionDataIntegrityException
    {
        try {
            return Instant.parse(sourceTimestamp);
        } catch (DateTimeParseException e) {
            throw new TransitionDataIntegrityException();
        }
    }
}
