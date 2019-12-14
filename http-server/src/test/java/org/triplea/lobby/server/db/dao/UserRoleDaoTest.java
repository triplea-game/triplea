package org.triplea.lobby.server.db.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.lobby.server.db.data.UserRole;

@DataSet(cleanBefore = true, value = "user_role/initial.yml")
class UserRoleDaoTest extends DaoTest {

  private final UserRoleDao userRoleDao = JdbiDatabase.newConnection().onDemand(UserRoleDao.class);

  @Test
  void lookupAnonymousRoleId() {
    assertThat(userRoleDao.lookupRoleId(UserRole.ANONYMOUS), is(1));
  }

  @Test
  void lookupHostRoleId() {
    assertThat(userRoleDao.lookupRoleId(UserRole.HOST), is(2));
  }
}
