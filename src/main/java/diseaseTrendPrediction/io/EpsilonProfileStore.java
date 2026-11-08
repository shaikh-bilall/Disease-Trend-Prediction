package diseaseTrendPrediction.io;

import com.google.common.base.Preconditions;
import diseaseTrendPrediction.io.exceptions.EpsilonProfileNotFoundException;
import diseaseTrendPrediction.io.transitionDataStorage.Database;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Loads persisted numeric tolerance profiles. */
public final class EpsilonProfileStore {

    private final Path databasePath;

    /**
     * Creates an epsilon profile store for the supplied database.
     *
     * @param databasePath SQLite database file
     */
    public EpsilonProfileStore(
            final Path databasePath
    ) {
        Preconditions.checkNotNull(databasePath, "Database path must not be null");
        this.databasePath = databasePath;
        checkEpsilonProfileStore();
    }

    /**
     * Loads the named epsilon profile.
     *
     * @param profileName profile key
     * @return configured epsilon value
     * @throws SQLException if the database query fails
     * @throws EpsilonProfileNotFoundException if the profile is not present
     */
    public double load(
            final String profileName
    )
            throws SQLException
    {
        checkEpsilonProfileStore();
        Preconditions.checkNotNull(profileName, "Profile name must not be null");
        try (Connection connection = Database.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT value FROM epsilon_profile WHERE name = ?")) {
            statement.setString(1, profileName);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new EpsilonProfileNotFoundException();
                }
                return result.getDouble("value");
            }
        }
    }

    private void checkEpsilonProfileStore() {
        Preconditions.checkNotNull(databasePath, "Database path must not be null");
    }
}
