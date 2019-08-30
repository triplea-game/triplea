package org.triplea.lobby.server.db.dao;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.test.common.Integration;

@Integration
@ExtendWith(DBUnitExtension.class)
@DataSet(cleanBefore = true, value = "user/select.yml")
class UserJdbiDaoTest {

  private static final String USERNAME = "user";
  private static final String PASSWORD =
      "$2a$56789_123456789_123456789_123456789_123456789_123456789_";

  private final UserJdbiDao userDao = JdbiDatabase.newConnection().onDemand(UserJdbiDao.class);

  @Test
  void lookupUserIdByName() {
    assertThat(userDao.lookupUserIdByName("DNE"), isEmpty());
    assertThat(userDao.lookupUserIdByName(USERNAME), isPresentAndIs(900000));
  }

  @Test
  void getPassword() {
    assertThat(userDao.getPassword(USERNAME), isPresentAndIs(PASSWORD));
    assertThat(userDao.getPassword("DNE"), isEmpty());
  }

  @Test
  void updateLastLogin() {
    assertThat(userDao.updateLastLoginTime(USERNAME), is(1));
    assertThat(userDao.updateLastLoginTime("DNE"), is(0));
  }
}
