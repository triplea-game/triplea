package org.triplea.lobby.server.db.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessLogDaoDataTest {
  private static final Instant NOW = Instant.now();
  private static final Timestamp timestamp = Timestamp.from(NOW);

  private static final String USERNAME = "The dark skiff greedily drinks the anchor.";
  private static final String MAC = "The woodchuck fights with grace, blow the freighter before it dies.";
  private static final String IP = "Yo-ho-ho, ye rainy freebooter- set sails for faith!";

  @Mock
  private ResultSet resultSet;

  @Test
  void buildResultMapper() throws Exception {
    when(resultSet.getBoolean(AccessLogDaoData.REGISTERED_COLUMN)).thenReturn(true);
    when(resultSet.getString(AccessLogDaoData.USERNAME_COLUMN)).thenReturn(USERNAME);
    when(resultSet.getString(AccessLogDaoData.MAC_COLUMN)).thenReturn(MAC);
    when(resultSet.getString(AccessLogDaoData.IP_COLUMN)).thenReturn(IP);
    when(resultSet.getTimestamp(eq(AccessLogDaoData.ACCESS_TIME_COLUMN), any(Calendar.class)))
        .thenReturn(timestamp);

    final AccessLogDaoData result = AccessLogDaoData.buildResultMapper().map(resultSet, null);

    assertThat(result.isRegistered(), is(true));
    assertThat(result.getUsername(), is(USERNAME));
    assertThat(result.getMac(), is(MAC));
    assertThat(result.getIp(), is(IP));
    assertThat(result.getAccessTime(), is(NOW));
  }
}
