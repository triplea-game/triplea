package org.triplea.db.dao.user.role;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.triplea.modules.http.LobbyServerTest;

@DataSet(cleanBefore = true, value = "user_role/initial.yml")
@RequiredArgsConstructor
class UserRoleDaoTest extends LobbyServerTest {

  private final UserRoleDao userRoleDao;

  @Test
  void lookupAnonymousRoleId() {
    assertThat(userRoleDao.lookupRoleId(UserRole.ANONYMOUS), is(1));
  }

  @Test
  void lookupHostRoleId() {
    assertThat(userRoleDao.lookupRoleId(UserRole.HOST), is(2));
  }
}
