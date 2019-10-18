package org.triplea.lobby.server.db.dao.api.key;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.db.dao.DaoTest;
import org.triplea.lobby.server.db.data.ApiKeyUserData;
import org.triplea.lobby.server.db.data.UserRole;

@DataSet(cleanBefore = true, value = "api_key/initial.yml")
class ApiKeyDaoTest extends DaoTest {

  private static final int USER_ID = 50;

  private static final ApiKeyUserData EXPECTED_MODERATOR_DATA =
      ApiKeyUserData.builder()
          .userId(USER_ID)
          .username("registered-user")
          .role(UserRole.MODERATOR)
          .build();
  private static final ApiKeyUserData EXPECTED_ANONYMOUS_DATA =
      ApiKeyUserData.builder().username("some-other-name").role(UserRole.ANONYMOUS).build();
  private static final ApiKeyUserData EXPECTED_HOST_DATA =
      ApiKeyUserData.builder().role(UserRole.HOST).build();

  private final ApiKeyDao apiKeyDao = DaoTest.newDao(ApiKeyDao.class);

  @Nested
  final class LookupByApiKey {

    @Test
    void keyNotFound() {
      assertThat(apiKeyDao.lookupByApiKey("key-does-not-exist"), isEmpty());
    }

    @Test
    void registeredUser() {
      final Optional<ApiKeyUserData> result = apiKeyDao.lookupByApiKey("zapi-key1");

      assertThat(result, isPresentAndIs(EXPECTED_MODERATOR_DATA));
    }

    @Test
    void anonymousUser() {
      final Optional<ApiKeyUserData> result = apiKeyDao.lookupByApiKey("zapi-key2");

      assertThat(result, isPresentAndIs(EXPECTED_ANONYMOUS_DATA));
    }

    @Test
    void hostUser() {
      final Optional<ApiKeyUserData> result = apiKeyDao.lookupByApiKey("zapi-key3");

      assertThat(result, isPresentAndIs(EXPECTED_HOST_DATA));
    }
  }

  // consult initial data set for expected user_role identifiers
  @DataSet("api_key/initial.yml")
  @Nested
  class StoreKey {

    @Test
    @ExpectedDataSet(
        value = "api_key/post_store_registered.yml",
        orderBy = "key",
        ignoreCols = {"id", "date_created"})
    void storeKeyRegisteredUser() {
      // name of the registered user and role_id do not have to strictly match what is in the
      // lobby_user table, but we would expect it to match as we find user role id and user id by
      // lookup from lobby_user table by username.
      assertThat(
          apiKeyDao.storeKey(50, "registered-user-name", "registered-user-key", "127.0.0.1", 1),
          is(1));
    }

    @Test
    @ExpectedDataSet(
        value = "api_key/post_store_anonymous.yml",
        orderBy = "key",
        ignoreCols = {"id", "date_created"})
    void storeKeyAnonymousUser() {
      assertThat(
          apiKeyDao.storeKey(null, "anonymous-user-name", "anonymous-user-key", "127.0.0.1", 3),
          is(1));
    }

    @Test
    @ExpectedDataSet(
        value = "api_key/post_store_host.yml",
        orderBy = "key",
        ignoreCols = {"id", "date_created"})
    void storeKeyHostUser() {
      assertThat(apiKeyDao.storeKey(null, null, "host-user-key", "127.0.0.1", 4), is(1));
    }
  }

  @Test
  @ExpectedDataSet(value = "api_key/post_delete.yml", orderBy = "key")
  void deleteOldKeys() {
    apiKeyDao.deleteOldKeys();
  }
}
