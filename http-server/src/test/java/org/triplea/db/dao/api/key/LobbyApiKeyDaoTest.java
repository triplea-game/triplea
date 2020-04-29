package org.triplea.db.dao.api.key;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.triplea.db.dao.DaoTest;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;

@DataSet(cleanBefore = true, value = "lobby_api_key/initial.yml")
class LobbyApiKeyDaoTest extends DaoTest {

  private static final int USER_ID = 50;

  private static final ApiKeyLookupRecord EXPECTED_MODERATOR_DATA =
      ApiKeyLookupRecord.builder()
          .userId(USER_ID)
          .username("registered-user")
          .role(UserRole.MODERATOR)
          .playerChatId("chat-id0")
          .apiKeyId(1000)
          .build();
  private static final ApiKeyLookupRecord EXPECTED_ANONYMOUS_DATA =
      ApiKeyLookupRecord.builder()
          .username("some-other-name")
          .role(UserRole.ANONYMOUS)
          .playerChatId("chat-id1")
          .apiKeyId(1001)
          .build();

  private final LobbyApiKeyDao lobbyApiKeyDao = DaoTest.newDao(LobbyApiKeyDao.class);

  @Test
  void keyNotFound() {
    assertThat(lobbyApiKeyDao.lookupByApiKey("key-does-not-exist"), isEmpty());
  }

  @Test
  void registeredUser() {
    final Optional<ApiKeyLookupRecord> result = lobbyApiKeyDao.lookupByApiKey("zapi-key1");

    assertThat(result, isPresentAndIs(EXPECTED_MODERATOR_DATA));
  }

  @Test
  void anonymousUser() {
    final Optional<ApiKeyLookupRecord> result = lobbyApiKeyDao.lookupByApiKey("zapi-key2");

    assertThat(result, isPresentAndIs(EXPECTED_ANONYMOUS_DATA));
  }

  @Test
  @DataSet(cleanBefore = true, value = "lobby_api_key/store_key_before.yml")
  @ExpectedDataSet(
      value = "lobby_api_key/store_key_after.yml",
      orderBy = "key",
      ignoreCols = {"id", "date_created"})
  void storeKey() {
    // name of the registered user and role_id do not have to strictly match what is in the
    // lobby_user table, but we would expect it to match as we find user role id and user id by
    // lookup from lobby_user table by username.
    assertThat(
        lobbyApiKeyDao.storeKey(
            "registered-user-name",
            50,
            1,
            "player-chat-id",
            "registered-user-key",
            "system-id",
            "127.0.0.1"),
        is(1));
  }

  @Test
  @DataSet(cleanBefore = true, value = "lobby_api_key/delete_old_keys_before.yml")
  @ExpectedDataSet(value = "lobby_api_key/delete_old_keys_after.yml", orderBy = "key")
  void deleteOldKeys() {
    lobbyApiKeyDao.deleteOldKeys();
  }

  @Test
  void lookupByPlayerChatId() {
    final Optional<GamePlayerLookup> playerIdLookup =
        lobbyApiKeyDao.lookupByPlayerChatId("chat-id0");

    assertThat(
        playerIdLookup,
        isPresentAndIs(
            GamePlayerLookup.builder()
                .userName(UserName.of("registered-user"))
                .systemId(SystemId.of("system-id0"))
                .ip("127.0.0.1")
                .build()));
  }
}
