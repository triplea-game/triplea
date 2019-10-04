package org.triplea.lobby.server.db.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyDaoDataTest {
  private static final String ROLE = "Plunders are the scallywags of the scrawny grace.";
  private static final int USER_ID = 55;

  @Mock private ResultSet resultSet;

  @Test
  void buildResultMapper() throws Exception {
    when(resultSet.getInt(ApiKeyUserData.USER_ID_COLUMN)).thenReturn(USER_ID);
    when(resultSet.getString(ApiKeyUserData.ROLE_COLUMN)).thenReturn(ROLE);

    final ApiKeyUserData result = ApiKeyUserData.buildResultMapper().map(resultSet, null);

    assertThat(result.getUserId(), is(USER_ID));
    assertThat(result.getRole(), is(ROLE));
  }
}
