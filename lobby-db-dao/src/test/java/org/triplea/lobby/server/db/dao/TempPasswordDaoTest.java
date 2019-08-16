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
@DataSet("temp_password/sample.yml")
class TempPasswordDaoTest {

  private static final String USERNAME = "username";
  private static final String EMAIL = "email";
  private static final int USER_ID = 500000;

  private static final String PASSWORD = "temp";
  private static final String NEW_PASSWORD = "new-temp";

  private final TempPasswordDao tempPasswordDao =
      JdbiDatabase.newConnection().onDemand(TempPasswordDao.class);

  @Test
  void fetchTempPassword() {
    assertThat(tempPasswordDao.fetchTempPassword(USERNAME), isEmpty());
    assertThat(tempPasswordDao.fetchTempPassword("DNE"), isEmpty());
    tempPasswordDao.insertTempPassword(USERNAME, PASSWORD);
    assertThat(tempPasswordDao.fetchTempPassword(USERNAME), isPresentAndIs(PASSWORD));
  }

  @Test
  void lookupUserIdByUsername() {
    assertThat(tempPasswordDao.lookupUserIdByUsername(USERNAME), isPresentAndIs(USER_ID));
    assertThat(tempPasswordDao.lookupUserIdByUsername("DNE"), isEmpty());
  }

  @Test
  void insertPasswordReturnsFalseIfUserNotFound() {
    assertThat(tempPasswordDao.insertTempPassword("DNE", PASSWORD), is(false));
  }

  @Test
  void insertTempPassword() {
    assertThat(tempPasswordDao.insertTempPassword(USERNAME, NEW_PASSWORD), is(true));
    assertThat(tempPasswordDao.fetchTempPassword(USERNAME), isPresentAndIs(NEW_PASSWORD));
  }

  @Test
  void invalidateTempPasswordsForMissingNameDoesNothing() {
    tempPasswordDao.insertTempPassword(USERNAME, PASSWORD);
    assertThat(tempPasswordDao.fetchTempPassword(USERNAME), isPresentAndIs(PASSWORD));
    tempPasswordDao.invalidateTempPasswords("DNE");
    assertThat(tempPasswordDao.fetchTempPassword(USERNAME), isPresentAndIs(PASSWORD));
  }

  @Test
  void invalidatePassword() {
    tempPasswordDao.invalidateTempPasswords(USERNAME);
    assertThat(tempPasswordDao.fetchTempPassword(USERNAME), isEmpty());
  }

  @Test
  void lookupUserEmail() {
    assertThat(tempPasswordDao.lookupUserEmail(USERNAME), isPresentAndIs(EMAIL));
    assertThat(tempPasswordDao.lookupUserEmail("DNE"), isEmpty());
  }
}
