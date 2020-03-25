package org.triplea.db.dao.access.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.triplea.db.dao.access.log.AccessLogRecord.ACCESS_TIME_COLUMN;
import static org.triplea.db.dao.access.log.AccessLogRecord.IP_COLUMN;
import static org.triplea.db.dao.access.log.AccessLogRecord.REGISTERED_COLUMN;
import static org.triplea.db.dao.access.log.AccessLogRecord.SYSTEM_ID_COLUMN;
import static org.triplea.db.dao.access.log.AccessLogRecord.USERNAME_COLUMN;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessLogRecordTest {
  private static final Instant NOW = Instant.now();
  private static final Timestamp timestamp = Timestamp.from(NOW);

  private static final String USERNAME = "The dark skiff greedily drinks the anchor.";
  private static final String MAC =
      "The woodchuck fights with grace, blow the freighter before it dies.";
  private static final String IP = "Yo-ho-ho, ye rainy freebooter- set sails for faith!";

  @Mock private ResultSet resultSet;

  @Test
  void buildResultMapper() throws Exception {
    when(resultSet.getBoolean(REGISTERED_COLUMN)).thenReturn(true);
    when(resultSet.getString(USERNAME_COLUMN)).thenReturn(USERNAME);
    when(resultSet.getString(SYSTEM_ID_COLUMN)).thenReturn(MAC);
    when(resultSet.getString(IP_COLUMN)).thenReturn(IP);
    when(resultSet.getTimestamp(eq(ACCESS_TIME_COLUMN), any(Calendar.class))).thenReturn(timestamp);

    final AccessLogRecord result = AccessLogRecord.buildResultMapper().map(resultSet, null);

    assertThat(result.isRegistered(), is(true));
    assertThat(result.getUsername(), is(USERNAME));
    assertThat(result.getSystemId(), is(MAC));
    assertThat(result.getIp(), is(IP));
    assertThat(result.getAccessTime(), is(NOW));
  }
}
