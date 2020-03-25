package org.triplea.db.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModeratorAuditHistoryDaoDataTest {

  private static final Instant NOW = Instant.now();
  private static final Timestamp timestamp = Timestamp.from(NOW);

  private static final String USER_NAME = "When the captain stutters for tortuga, wet landlubbers.";
  private static final String ACTION_NAME =
      "Cannibals wave with endurance at the undead port royal!";
  private static final String ACTION_TARGET =
      "The comrade hauls with malaria, trade the quarter-deck until it falls.";

  @Mock private ResultSet resultSet;

  @Test
  void verifyMapping() throws Exception {
    when(resultSet.getTimestamp(eq(ModeratorAuditHistoryDaoData.DATE_CREATED_COLUMN), any()))
        .thenReturn(timestamp);
    when(resultSet.getString(ModeratorAuditHistoryDaoData.USER_NAME_COLUMN)).thenReturn(USER_NAME);
    when(resultSet.getString(ModeratorAuditHistoryDaoData.ACTION_NAME_COLUMN))
        .thenReturn(ACTION_NAME);
    when(resultSet.getString(ModeratorAuditHistoryDaoData.ACTION_TARGET_COLUMN))
        .thenReturn(ACTION_TARGET);

    final ModeratorAuditHistoryDaoData result =
        ModeratorAuditHistoryDaoData.buildResultMapper().map(resultSet, null);

    assertThat(result.getUsername(), is(USER_NAME));
    assertThat(result.getDateCreated(), is(NOW));
    assertThat(result.getActionName(), is(ACTION_NAME));
    assertThat(result.getActionTarget(), is(ACTION_TARGET));
  }
}
