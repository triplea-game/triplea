package org.triplea.db.dao.temp.password;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.db.LobbyModuleDatabaseTestSupport;
import org.triplea.test.common.RequiresDatabase;

@DataSet(
    value =
        "temp_password/user_role.yml,"
            + "temp_password/lobby_user.yml,"
            + "temp_password/temp_password_request.yml",
    useSequenceFiltering = false)
@RequiredArgsConstructor
@ExtendWith(LobbyModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class TempPasswordDaoTest {

  private static final String USERNAME = "username";
  private static final String EMAIL = "email@";
  private static final int USER_ID = 500000;

  private static final String PASSWORD = "temp";
  private static final String NEW_PASSWORD = "new-temp";

  private final TempPasswordDao tempPasswordDao;

  @Test
  void fetchTempPassword() {
    assertThat(tempPasswordDao.fetchTempPassword(USERNAME), isEmpty());
    assertThat(tempPasswordDao.fetchTempPassword("DNE"), isEmpty());
    tempPasswordDao.insertTempPassword(USERNAME, EMAIL, PASSWORD);
    assertThat(tempPasswordDao.fetchTempPassword(USERNAME), isPresentAndIs(PASSWORD));
  }

  @Test
  void lookupUserIdByUsernameAndEmail() {
    assertThat(
        tempPasswordDao.lookupUserIdByUsernameAndEmail(USERNAME, EMAIL), isPresentAndIs(USER_ID));
    assertThat(tempPasswordDao.lookupUserIdByUsernameAndEmail("DNE", "DNE"), isEmpty());
  }

  @Test
  void lookupUserIdByUsername() {
    assertThat(tempPasswordDao.lookupUserIdByUsername(USERNAME), isPresentAndIs(USER_ID));
    assertThat(tempPasswordDao.lookupUserIdByUsername("DNE"), isEmpty());
  }

  @Test
  void insertPasswordReturnsFalseIfUserNotFound() {
    assertThat(tempPasswordDao.insertTempPassword("DNE", "DNE", PASSWORD), is(false));
  }

  @Test
  void insertTempPassword() {
    assertThat(tempPasswordDao.insertTempPassword(USERNAME, EMAIL, NEW_PASSWORD), is(true));
    assertThat(tempPasswordDao.fetchTempPassword(USERNAME), isPresentAndIs(NEW_PASSWORD));
  }

  @Test
  void invalidateTempPasswordsForMissingNameDoesNothing() {
    // verify that before we do any invalidation that indeed our known user has a temp password
    assertThat(
        tempPasswordDao.fetchTempPassword("user-with-temp-password"), isPresentAndIs(PASSWORD));

    // invalidate password for some other user
    tempPasswordDao.invalidateTempPasswords("DNE");

    // expect the temp password for the known user to still exist
    assertThat(
        tempPasswordDao.fetchTempPassword("user-with-temp-password"), isPresentAndIs(PASSWORD));
  }

  @Test
  void invalidatePassword() {
    tempPasswordDao.invalidateTempPasswords("user-with-temp-password");
    assertThat(
        "With password invalidated, fetching temp password should return empty",
        tempPasswordDao.fetchTempPassword("user-with-temp-password"),
        isEmpty());
  }
}
