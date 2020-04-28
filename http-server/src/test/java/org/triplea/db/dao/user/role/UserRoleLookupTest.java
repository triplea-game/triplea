package org.triplea.db.dao.user.role;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRoleLookupTest {
  private static final int USER_ID = 20;
  private static final int USER_ROLE_ID = 100;

  @Mock private ResultSet resultSet;

  @Test
  void buildResultMapper() throws Exception {
    when(resultSet.getInt(UserRoleLookup.USER_ID_COLUMN)).thenReturn(USER_ID);
    when(resultSet.getInt(UserRoleLookup.USER_ROLE_ID_COLUMN)).thenReturn(USER_ROLE_ID);

    final UserRoleLookup result = UserRoleLookup.buildResultMapper().map(resultSet, null);

    assertThat(result.getUserId(), is(USER_ID));
    assertThat(result.getUserRoleId(), is(USER_ROLE_ID));
  }
}
