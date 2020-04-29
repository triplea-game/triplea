package org.triplea.db.dao.moderator;

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
class ModeratorUserDaoDataTest {
  private static final Instant NOW = Instant.now();
  private static final Timestamp timestamp = Timestamp.from(NOW);
  private static final String USERNAME =
      "The girl views with strength, hoist the pacific ocean until it laughs.";

  @Mock private ResultSet resultSet;

  @Test
  void buildResultMapper() throws Exception {
    when(resultSet.getTimestamp(eq(ModeratorUserDaoData.LAST_LOGIN_COLUMN), any(Calendar.class)))
        .thenReturn(timestamp);
    when(resultSet.getString(ModeratorUserDaoData.USERNAME_COLUMN)).thenReturn(USERNAME);

    final ModeratorUserDaoData result =
        ModeratorUserDaoData.buildResultMapper().map(resultSet, null);

    assertThat(result.getUsername(), is(USERNAME));
    assertThat(result.getLastLogin(), is(NOW));
  }
}
