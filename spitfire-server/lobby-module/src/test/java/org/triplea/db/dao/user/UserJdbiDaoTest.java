package org.triplea.db.dao.user;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.db.LobbyModuleDatabaseTestSupport;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.db.dao.user.role.UserRoleLookup;
import org.triplea.test.common.RequiresDatabase;

@DataSet(value = "user/user_role.yml,user/lobby_user.yml", useSequenceFiltering = false)
@RequiredArgsConstructor
@ExtendWith(LobbyModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class UserJdbiDaoTest {

  private static final int USER_ID = 900000;
  private static final String USERNAME = "user";
  private static final String NEW_USERNAME = "new-user";

  private static final String EMAIL = "user@email.com";
  private static final String PASSWORD =
      "$2a$56789_123456789_123456789_123456789_123456789_123456789_";
  private static final String NEW_PASSWORD =
      "$2a$abcde_123456789_123456789_123456789_123456789_123456789_";

  private final UserJdbiDao userDao;

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

  @ExpectedDataSet("user/change_password_after.yml")
  @Test
  void updatePassword() {
    assertThat(userDao.updatePassword(USER_ID, NEW_PASSWORD), is(1));
  }

  @Test
  void fetchEmail() {
    assertThat(userDao.fetchEmail(USER_ID), is("email@"));
  }

  @ExpectedDataSet("user/post_change_email.yml")
  @Test
  void updateEmail() {
    userDao.updateEmail(USER_ID, "new-email@");
  }

  @DataSet(value = "user/user_role.yml,user/empty_lobby_user.yml", useSequenceFiltering = false)
  @ExpectedDataSet(value = "user/create_user_after.yml", orderBy = "id", ignoreCols = "id")
  @Test
  void createUser() {
    userDao.createUser(NEW_USERNAME, EMAIL, PASSWORD);
  }

  @Test
  void lookupUserRoleIdByName() {
    assertThat(userDao.lookupUserIdAndRoleIdByUserName("does-not-exist"), isEmpty());
    assertThat(
        userDao.lookupUserIdAndRoleIdByUserName(USERNAME),
        isPresentAndIs(UserRoleLookup.builder().userId(USER_ID).userRoleId(1).build()));
  }

  @Test
  void lookupUserRoleByUserName() {
    assertThat(userDao.lookupUserRoleByUserName("does-not-exist"), isEmpty());
    assertThat(userDao.lookupUserRoleByUserName(USERNAME), isPresentAndIs(UserRole.PLAYER));
  }
}
