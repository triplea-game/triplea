package org.triplea.lobby.server.db;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.test.common.Integration;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;

@ExtendWith(DBUnitExtension.class)
@Integration
class ApiKeyRegistrationDaoTest {

  private static final int MODERATOR_ID = 300000;

  private static final ApiKeyRegistrationDao API_KEY_REGISTRATION_DAO =
      JdbiDatabase.newConnection().onDemand(ApiKeyRegistrationDao.class);
  private static final String SECRET_KEY =
      "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5"
          + "976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";

  private static final String OLD_KEY =
      "aaaaaaabbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5"
          + "976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";

  private static final String ALREADY_USED_KEY =
      "bbbbbbabbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5"
          + "976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";


  @Test
  @DataSet("api_key_registration/lookup_moderator_by_single_use.yml")
  void lookupModeratorBySingleUseKey() {
    assertThat(
        API_KEY_REGISTRATION_DAO.lookupModeratorBySingleUseKey(SECRET_KEY),
        isPresentAndIs(MODERATOR_ID));

    assertThat(
        API_KEY_REGISTRATION_DAO.lookupModeratorBySingleUseKey("not found"),
        isEmpty());
  }

  @Test
  @DataSet("api_key_registration/invalidate_single_use_key.yml")
  void invalidateSingleUseKeyOnlyInvalidatesOnce() {
    assertThat(API_KEY_REGISTRATION_DAO.invalidateSingleUseKey(SECRET_KEY), is(1));
    assertThat(API_KEY_REGISTRATION_DAO.invalidateSingleUseKey(SECRET_KEY), is(0));
  }


  @Test
  @DataSet("api_key_registration/invalidate_single_use_key.yml")
  void invalidateSingleUseKeyAlreadyInvalidCases() {
    assertThat(API_KEY_REGISTRATION_DAO.invalidateSingleUseKey("does not match"), is(0));
    assertThat(API_KEY_REGISTRATION_DAO.invalidateSingleUseKey(OLD_KEY), is(0));
    assertThat(API_KEY_REGISTRATION_DAO.invalidateSingleUseKey(ALREADY_USED_KEY), is(0));
  }


  @Test
  @DataSet("api_key_registration/pre_insert_single_use_key.yml")
  @ExpectedDataSet("api_key_registration/post_insert_single_use_key.yml")
  void insertNewSingleUseKey() {
    API_KEY_REGISTRATION_DAO.insertNewSingleUseKey(MODERATOR_ID, SECRET_KEY);
  }

  @Test
  @DataSet("api_key_registration/pre_insert_api_key.yml")
  @ExpectedDataSet("api_key_registration/post_insert_api_key.yml")
  void insertApiKey() {
    API_KEY_REGISTRATION_DAO.insertNewApiKey(MODERATOR_ID, SECRET_KEY);
  }


  @Test
  @DataSet("api_key_registration/pre_invalidate_and_insert_new.yml")
  @ExpectedDataSet("api_key_registration/post_invalidate_and_insert_new.yml")
  void invalidateOldKeyAndInsertNew() {
    API_KEY_REGISTRATION_DAO.invalidateOldKeyAndInsertNew(
        MODERATOR_ID, OLD_KEY, SECRET_KEY);
  }


  // pre and post datasets expected to be the same, checking error cases and all transactions
  // should be rolled back.
  @Test
  @DataSet("api_key_registration/pre_invalidate_and_insert_new.yml")
  @ExpectedDataSet("api_key_registration/pre_invalidate_and_insert_new.yml")
  void invalidateOldKeyAndInsertNewErrorChecking() {
    assertThrows(
        IllegalStateException.class,
        () -> API_KEY_REGISTRATION_DAO.invalidateOldKeyAndInsertNew(-1, OLD_KEY, SECRET_KEY));

    assertThrows(
        NullPointerException.class,
        () -> API_KEY_REGISTRATION_DAO.invalidateOldKeyAndInsertNew(MODERATOR_ID, null, SECRET_KEY));

    assertThrows(
        NullPointerException.class,
        () -> API_KEY_REGISTRATION_DAO.invalidateOldKeyAndInsertNew(MODERATOR_ID, OLD_KEY, null));

    assertThrows(
        IllegalStateException.class,
        () -> API_KEY_REGISTRATION_DAO.invalidateOldKeyAndInsertNew(MODERATOR_ID, "too short", SECRET_KEY));

    assertThrows(
        IllegalStateException.class,
        () -> API_KEY_REGISTRATION_DAO.invalidateOldKeyAndInsertNew(MODERATOR_ID, OLD_KEY, "too short"));

    // old and new key cannot match
    assertThrows(
        IllegalStateException.class,
        () -> API_KEY_REGISTRATION_DAO.invalidateOldKeyAndInsertNew(MODERATOR_ID, OLD_KEY, OLD_KEY));

    // already used key is not found, we must find the old key and invalidate it.
    assertThrows(
        IllegalStateException.class,
        () -> API_KEY_REGISTRATION_DAO.invalidateOldKeyAndInsertNew(MODERATOR_ID, ALREADY_USED_KEY, SECRET_KEY));
  }
}
