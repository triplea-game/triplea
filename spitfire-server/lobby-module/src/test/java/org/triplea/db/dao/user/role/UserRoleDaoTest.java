package org.triplea.db.dao.user.role;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.db.LobbyModuleDatabaseTestSupport;
import org.triplea.test.common.RequiresDatabase;

@DataSet(value = "user_role/initial.yml", useSequenceFiltering = false)
@RequiredArgsConstructor
@ExtendWith(LobbyModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class UserRoleDaoTest {

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
