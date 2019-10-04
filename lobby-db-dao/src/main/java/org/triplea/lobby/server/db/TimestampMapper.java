package org.triplea.lobby.server.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.Optional;
import java.util.TimeZone;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class to map {@code java.sql.TimeStamp} objects from {@code java.sql.ResultSet} objects
 * to {@code java.time.Instant}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimestampMapper {
  public static Instant map(final ResultSet resultSet, final String columnName)
      throws SQLException {
    final Calendar cal = Calendar.getInstance();
    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    // TODO: verify the null handling here in test
    return Optional.ofNullable(resultSet.getTimestamp(columnName, cal))
        .map(Timestamp::toInstant)
        .orElse(null);
  }
}
