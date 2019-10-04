package org.triplea.lobby.server.db.dao;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.lobby.server.db.data.ApiKeyUserData;
import org.triplea.test.common.Integration;

@ExtendWith(DBUnitExtension.class)
@Integration
@DataSet("api_key/initial.yml")
class ApiKeyDaoTest {

  private static final int USER_ID = 50;

  private static final ApiKeyUserData EXPECTED_MODERATOR_DATA =
      ApiKeyUserData.builder().userId(USER_ID).role("MODERATOR").build();

  private static final ApiKeyUserData EXPECTED_ANONYMOUS_DATA =
      ApiKeyUserData.builder().role("ANONYMOUS").build();

  private final ApiKeyDao apiKeyDao = JdbiDatabase.newConnection().onDemand(ApiKeyDao.class);

  @Nested
  final class LookupRole {

    @Test
    void keyNotFound() {
      assertThat(apiKeyDao.lookupByApiKey("key-does-not-exist"), isEmpty());
    }

    @Test
    void registeredUser() {
      final Optional<ApiKeyUserData> result = apiKeyDao.lookupByApiKey("api-key1");

      assertThat(result, isPresentAndIs(EXPECTED_MODERATOR_DATA));
    }

    @Test
    void anonymousUser() {
      final Optional<ApiKeyUserData> result = apiKeyDao.lookupByApiKey("api-key2");

      assertThat(result, isPresentAndIs(EXPECTED_ANONYMOUS_DATA));
    }
  }

  @Test
  @ExpectedDataSet(value = "api_key/post_store.yml", orderBy = "key")
  void storeKey() {
    apiKeyDao.storeKey(null, "anonymous-user-key");
    apiKeyDao.storeKey(USER_ID, "registered-user-key");
  }

  @Test
  @ExpectedDataSet("api_key/post_delete.yml")
  void deleteOldKeys() {
    apiKeyDao.deleteOldKeys();
  }
}
