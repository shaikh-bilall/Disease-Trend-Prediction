package diseaseTrendPrediction.io.transitionDataStorage;

import com.google.common.base.Preconditions;
import diseaseTrendPrediction.io.exceptions.DatabaseConnectionException;
import diseaseTrendPrediction.io.exceptions.DatabaseDirectoryCreationException;
import diseaseTrendPrediction.io.exceptions.DatabaseInitializationException;
import diseaseTrendPrediction.io.exceptions.UnsupportedDatabaseSchemaException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
     * @throws DatabaseInitializationException if the database cannot be initialized
     * @throws UnsupportedDatabaseSchemaException if the database schema is newer
     * @throws DatabaseDirectoryCreationException if the database directory cannot be created
     */
    public static void initialize(
            final Path databasePath
    )
            throws DatabaseInitializationException,
            UnsupportedDatabaseSchemaException,
            DatabaseDirectoryCreationException
    {
        Preconditions.checkNotNull(
            databasePath,
            "Database path must not be null"
        );

        final Path parent = databasePath.toAbsolutePath().getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new DatabaseDirectoryCreationException();
            }
        }
        try (
            Connection connection = open(databasePath)
        ) {
            connection.setAutoCommit(false);
            try (
                Statement statement = connection.createStatement()
            ) {
                int version;
                try (
                    var result = statement.executeQuery(
                        "PRAGMA user_version"
                    )
                ) {
                    version = result.next() ? result.getInt(1) : 0;
                }
                if (version > SCHEMA_VERSION) {
                    throw new UnsupportedDatabaseSchemaException();
                }
                statement.execute(
                    "CREATE TABLE IF NOT EXISTS transition_snapshot(" +
                            "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                            "state_count INTEGER NOT NULL," +
                            "source_timestamp TEXT NOT NULL" +
                        ")"
                );
                createMatrixTable(statement, "transition_matrix");
                createMatrixTable(statement, "p_matrix");
                createMatrixTable(statement, "d_matrix");
                createMatrixTable(statement, "inverse_p_matrix");
                statement.execute(
                    "CREATE TABLE IF NOT EXISTS epsilon_profile(" +
                            "name TEXT PRIMARY KEY," +
                            "value REAL NOT NULL" +
                        ")"
                );
                seedEpsilonProfiles(connection);
                statement.execute(
                    "PRAGMA user_version = " + SCHEMA_VERSION
                );
                connection.commit();
            } catch (UnsupportedDatabaseSchemaException e) {
                connection.rollback();
                throw e;
            } catch (SQLException e) {
                connection.rollback();
                throw new DatabaseInitializationException();
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (UnsupportedDatabaseSchemaException e) {
            throw e;
        } catch (DatabaseConnectionException e) {
            throw new DatabaseInitializationException();
        } catch (SQLException e) {
            throw new DatabaseInitializationException();
        }
    }

    /**
     * Opens a connection to the supplied SQLite database file.
     *
     * @param databasePath SQLite database file
     * @return an open database connection
     * @throws DatabaseConnectionException if a connection cannot be opened
     */
    public static Connection open(
            final Path databasePath
    )
            throws DatabaseConnectionException
    {
        Preconditions.checkNotNull(
            databasePath,
            "Database path must not be null"
        );
        Connection connection;
        try {
            connection = DriverManager.getConnection(
                "jdbc:sqlite:" + databasePath.toAbsolutePath()
            );
        } catch (SQLException e) {
            throw new DatabaseConnectionException();
        }
        return connection;
    }

    // Creates one matrix cell table when it is absent.
    private static void createMatrixTable(
            final Statement statement,
            final String table
    )
            throws DatabaseInitializationException
    {
        try {
            statement.execute(
                "CREATE TABLE IF NOT EXISTS " + table + "(" +
                        "row_index INTEGER NOT NULL," +
                        "column_index INTEGER NOT NULL," +
                        "value REAL NOT NULL," +
                        "PRIMARY KEY (row_index, column_index)" +
                    ")"
            );
        } catch (SQLException e) {
            throw new DatabaseInitializationException();
        }
    }

    // Inserts default epsilon profiles without replacing existing values.
    private static void seedEpsilonProfiles(
            final Connection connection
    )
            throws DatabaseInitializationException
    {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "INSERT OR IGNORE INTO epsilon_profile(name, value) " +
                    "VALUES (?, ?)"
            )
        ) {
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
        } catch (SQLException e) {
            throw new DatabaseInitializationException();
        }
    }
}
