package diseaseTrendPrediction.io.transitionDataStorage;

import com.google.common.base.Preconditions;
import diseaseTrendPrediction.io.exceptions.UnsupportedDatabaseSchemaException;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/** Initializes and opens the application SQLite database. */
public final class Database {

    private static final int SCHEMA_VERSION = 1;

    // Prevents instances of this stateless utility.
    private Database() {}

    /**
     * Creates the schema and default epsilon profiles when absent.
     *
     * @param databasePath SQLite database file
     * @throws SQLException if the database cannot be initialized
     * @throws UnsupportedDatabaseSchemaException if the database schema is newer
     * @throws IOException if the database directory cannot be created
     * @throws NullPointerException if databasePath is null
     */
    public static void initialize(
            final Path databasePath
    )
            throws SQLException,
            UnsupportedDatabaseSchemaException,
            IOException,
            NullPointerException
    {
        Preconditions.checkNotNull(
                databasePath,
                "Database path must not be null"
        );
        final Path parent = databasePath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (Connection connection = open(databasePath)) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                int version;
                try (var result = statement.executeQuery("PRAGMA user_version")) {
                    version = result.next() ? result.getInt(1) : 0;
                }
                if (version > SCHEMA_VERSION) {
                    throw new UnsupportedDatabaseSchemaException();
                }
                statement.execute("CREATE TABLE IF NOT EXISTS transition_snapshot " +
                        "(id INTEGER PRIMARY KEY CHECK (id = 1), state_count INTEGER NOT NULL, " +
                        "source_timestamp TEXT NOT NULL)");
                createMatrixTable(statement, "transition_matrix");
                createMatrixTable(statement, "p_matrix");
                createMatrixTable(statement, "d_matrix");
                createMatrixTable(statement, "inverse_p_matrix");
                statement.execute("CREATE TABLE IF NOT EXISTS epsilon_profile " +
                        "(name TEXT PRIMARY KEY, value REAL NOT NULL)");
                seedEpsilonProfiles(connection);
                statement.execute("PRAGMA user_version = " + SCHEMA_VERSION);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Opens a connection to the supplied SQLite database file.
     *
     * @param databasePath SQLite database file
     * @return an open database connection
     * @throws SQLException if a connection cannot be opened
     * @throws NullPointerException if databasePath is null
     */
    public static Connection open(
            final Path databasePath
    )
            throws SQLException,
            NullPointerException
    {
        Preconditions.checkNotNull(
                databasePath,
                "Database path must not be null"
        );
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
    }

    // Creates one matrix cell table when it is absent.
    private static void createMatrixTable(
            final Statement statement,
            final String table
    )
            throws SQLException
    {
        statement.execute("CREATE TABLE IF NOT EXISTS " + table + " " +
                "(row_index INTEGER NOT NULL, column_index INTEGER NOT NULL, " +
                "value REAL NOT NULL, " +
                "PRIMARY KEY (row_index, column_index))");
    }

    // Inserts default epsilon profiles without replacing existing values.
    private static void seedEpsilonProfiles(
            final Connection connection
    )
            throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR IGNORE INTO epsilon_profile (name, value) VALUES (?, ?)")) {
            statement.setString(1, "strict");
            statement.setDouble(2, 1e-9);
            statement.addBatch();
            statement.setString(1, "mediocre");
            statement.setDouble(2, 1e-6);
            statement.addBatch();
            statement.setString(1, "loose");
            statement.setDouble(2, 1e-3);
            statement.addBatch();
            statement.executeBatch();
        }
    }
}
