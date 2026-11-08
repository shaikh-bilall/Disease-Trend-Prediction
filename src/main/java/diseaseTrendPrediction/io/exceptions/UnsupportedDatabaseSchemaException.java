package diseaseTrendPrediction.io.exceptions;

import java.sql.SQLException;

/** Signals that the database schema is newer than this application supports. */
public final class UnsupportedDatabaseSchemaException extends SQLException {
}
