package org.triplea.lobby.server.db.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class ApiKeyDaoDataTest {
  private static final Instant NOW = Instant.now();
  private static final Timestamp timestamp = Timestamp.from(NOW);

  private static final String PUBLIC_ID = "Plunders are the scallywags of the scrawny grace.";
  private static final String HOST = "Where is the misty bilge rat?";

  @Mock
  private ResultSet resultSet;

  @Test
  void buildResultMapper() throws Exception {
    when(resultSet.getString(ApiKeyDaoData.PUBLIC_ID_COLUMN)).thenReturn(PUBLIC_ID);
    when(resultSet.getString(ApiKeyDaoData.LAST_USED_HOST_ADDRESS_COLUMN)).thenReturn(HOST);
    when(resultSet.getTimestamp(eq(ApiKeyDaoData.DATE_LAST_USED_COLUMN), any(Calendar.class)))
        .thenReturn(timestamp);

    final ApiKeyDaoData result = ApiKeyDaoData.buildResultMapper().map(resultSet, null);

    assertThat(result.getPublicId(), is(PUBLIC_ID));
    assertThat(result.getLastUsedByHostAddress(), is(HOST));
    assertThat(result.getLastUsed(), is(NOW));
  }
}
