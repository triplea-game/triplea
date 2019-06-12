package org.triplea.lobby.server.db;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.test.common.Integration;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;

@ExtendWith(DBUnitExtension.class)
@Integration
@DataSet("api_key_validation/select.yml")
class ApiKeyValidationDaoTest {
  private static final String SECRET_KEY =
      "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5"
          + "976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";
  private static final int EXPECTED_MODERATOR_ID = 900000;

  private static final ApiKeyValidationDao API_KEY_VALIDATION_DAO =
      JdbiDatabase.newConnection().onDemand(ApiKeyValidationDao.class);

  @Test
  void lookupModeratorIdByApiKeySuccessCase() {
    assertThat(
        API_KEY_VALIDATION_DAO.lookupModeratorIdByApiKey(SECRET_KEY),
        isPresentAndIs(EXPECTED_MODERATOR_ID));
  }

  @Test
  void lookupModeratorIdByApiKeyNotFoundCase() {
    assertThat(
        API_KEY_VALIDATION_DAO.lookupModeratorIdByApiKey("invalid key"),
        isEmpty());
  }
}
