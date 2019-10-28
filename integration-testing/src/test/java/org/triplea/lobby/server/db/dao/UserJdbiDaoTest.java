package org.triplea.lobby.server.db.dao;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.db.data.UserRoleLookup;

@DataSet(cleanBefore = true, value = "user/select.yml")
class UserJdbiDaoTest extends DaoTest {

  private static final int USER_ID = 900000;
  private static final String USERNAME = "user";
  private static final String PASSWORD =
      "$2a$56789_123456789_123456789_123456789_123456789_123456789_";
  private static final String NEW_PASSWORD =
      "$2a$abcde_123456789_123456789_123456789_123456789_123456789_";
  private final UserJdbiDao userDao = DaoTest.newDao(UserJdbiDao.class);

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

  @Test
  @ExpectedDataSet("user/post_change_password.yml")
  void updatePassword() {
    assertThat(userDao.updatePassword(USER_ID, NEW_PASSWORD), is(1));
  }

  @Test
  void fetchEmail() {
    assertThat(userDao.fetchEmail(USER_ID), is("email@"));
  }

  @Test
  @ExpectedDataSet("user/post_change_email.yml")
  void updateEmail() {
    userDao.updateEmail(USER_ID, "new-email@");
  }

  @Test
  void lookupUserIdAndRoleIdForPlayerNameThatDoesNotExist() {
    assertThat(userDao.lookupUserIdAndRoleIdByUserName("DNE"), isEmpty());
  }

  @Test
  void lookupUserIdAndRoleId() {
    assertThat(
        userDao.lookupUserIdAndRoleIdByUserName(USERNAME),
        isPresentAndIs(UserRoleLookup.builder().userId(USER_ID).userRoleId(1).build()));
  }

  @Test
  @DataSet("user/create_user_before.yml")
  @ExpectedDataSet("user/create_user_after.yml")
  void createAccount() {
    userDao.createUser("user-name", "user-email@email.com", PASSWORD);
  }
}
