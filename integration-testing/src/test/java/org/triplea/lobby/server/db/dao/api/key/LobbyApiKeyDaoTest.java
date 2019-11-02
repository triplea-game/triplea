package org.triplea.lobby.server.db.dao.api.key;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.db.dao.DaoTest;
import org.triplea.lobby.server.db.data.ApiKeyUserData;
import org.triplea.lobby.server.db.data.UserRole;

@DataSet(cleanBefore = true, value = "lobby_api_key/initial.yml")
class LobbyApiKeyDaoTest extends DaoTest {

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

  private final LobbyApiKeyDao lobbyApiKeyDao = DaoTest.newDao(LobbyApiKeyDao.class);

  @Test
  void keyNotFound() {
    assertThat(lobbyApiKeyDao.lookupByApiKey("key-does-not-exist"), isEmpty());
  }

  @Test
  void registeredUser() {
    final Optional<ApiKeyUserData> result = lobbyApiKeyDao.lookupByApiKey("zapi-key1");

    assertThat(result, isPresentAndIs(EXPECTED_MODERATOR_DATA));
  }

  @Test
  void anonymousUser() {
    final Optional<ApiKeyUserData> result = lobbyApiKeyDao.lookupByApiKey("zapi-key2");

    assertThat(result, isPresentAndIs(EXPECTED_ANONYMOUS_DATA));
  }

  @Test
  void hostUser() {
    final Optional<ApiKeyUserData> result = lobbyApiKeyDao.lookupByApiKey("zapi-key3");

    assertThat(result, isPresentAndIs(EXPECTED_HOST_DATA));
  }

  @Test
  @ExpectedDataSet(
      value = "lobby_api_key/post_store_registered.yml",
      orderBy = "key",
      ignoreCols = {"id", "date_created"})
  void storeKeyRegisteredUser() {
    // name of the registered user and role_id do not have to strictly match what is in the
    // lobby_user table, but we would expect it to match as we find user role id and user id by
    // lookup from lobby_user table by username.
    assertThat(
        lobbyApiKeyDao.storeKey(50, "registered-user-name", "registered-user-key", "127.0.0.1", 1),
        is(1));
  }

  @Test
  @ExpectedDataSet(
      value = "lobby_api_key/post_store_anonymous.yml",
      orderBy = "key",
      ignoreCols = {"id", "date_created"})
  void storeKeyAnonymousUser() {
    assertThat(
        lobbyApiKeyDao.storeKey(null, "anonymous-user-name", "anonymous-user-key", "127.0.0.1", 3),
        is(1));
  }

  @Test
  @ExpectedDataSet(
      value = "lobby_api_key/post_store_host.yml",
      orderBy = "key",
      ignoreCols = {"id", "date_created"})
  void storeKeyHostUser() {
    assertThat(lobbyApiKeyDao.storeKey(null, null, "host-user-key", "127.0.0.1", 4), is(1));
  }

  @Test
  @ExpectedDataSet(value = "lobby_api_key/post_delete.yml", orderBy = "key")
  void deleteOldKeys() {
    lobbyApiKeyDao.deleteOldKeys();
  }
}
