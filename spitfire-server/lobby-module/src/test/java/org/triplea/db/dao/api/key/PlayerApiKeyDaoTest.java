package org.triplea.db.dao.api.key;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.db.LobbyModuleDatabaseTestSupport;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.test.common.RequiresDatabase;

@DataSet(
    value =
        "lobby_api_key/user_role.yml,"
            + "lobby_api_key/lobby_user.yml,"
            + "lobby_api_key/lobby_api_key.yml",
    useSequenceFiltering = false)
@RequiredArgsConstructor
@ExtendWith(LobbyModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class PlayerApiKeyDaoTest {

  private static final int USER_ID = 50;

  private static final PlayerApiKeyLookupRecord EXPECTED_MODERATOR_DATA =
      PlayerApiKeyLookupRecord.builder()
          .userId(USER_ID)
          .username("registered-user")
          .userRole(UserRole.MODERATOR)
          .playerChatId("chat-id0")
          .apiKeyId(1000)
          .build();
  private static final PlayerApiKeyLookupRecord EXPECTED_ANONYMOUS_DATA =
      PlayerApiKeyLookupRecord.builder()
          .username("some-other-name")
          .userRole(UserRole.ANONYMOUS)
          .playerChatId("chat-id1")
          .apiKeyId(1001)
          .build();

  private final PlayerApiKeyDao playerApiKeyDao;

  @Test
  void keyNotFound() {
    assertThat(playerApiKeyDao.lookupByApiKey("key-does-not-exist"), isEmpty());
  }

  @Test
  void registeredUser() {
    final Optional<PlayerApiKeyLookupRecord> result = playerApiKeyDao.lookupByApiKey("zapi-key1");

    assertThat(result, isPresentAndIs(EXPECTED_MODERATOR_DATA));
  }

  @Test
  void anonymousUser() {
    final Optional<PlayerApiKeyLookupRecord> result = playerApiKeyDao.lookupByApiKey("zapi-key2");

    assertThat(result, isPresentAndIs(EXPECTED_ANONYMOUS_DATA));
  }

  @Test
  @DataSet(
      value =
          "lobby_api_key/user_role.yml,"
              + "lobby_api_key/lobby_user.yml,"
              + "lobby_api_key/empty_lobby_api_key.yml",
      useSequenceFiltering = false)
  @ExpectedDataSet(
      value = "lobby_api_key/lobby_api_key_post_insert.yml",
      orderBy = "key",
      ignoreCols = {"id", "date_created"})
  void storeKey() {
    // name of the registered user and role_id do not have to strictly match what is in the
    // lobby_user table, but we would expect it to match as we find user role id and user id by
    // lookup from lobby_user table by username.
    assertThat(
        playerApiKeyDao.storeKey(
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
  @DataSet(
      value = "lobby_api_key/user_role.yml, lobby_api_key/delete_old_keys_before.yml",
      useSequenceFiltering = false)
  @ExpectedDataSet(value = "lobby_api_key/delete_old_keys_after.yml", orderBy = "key")
  void deleteOldKeys() {
    playerApiKeyDao.deleteOldKeys();
  }

  @Test
  void lookupByPlayerChatId() {
    final Optional<PlayerIdentifiersByApiKeyLookup> playerIdLookup =
        playerApiKeyDao.lookupByPlayerChatId("chat-id0");

    assertThat(
        playerIdLookup,
        isPresentAndIs(
            PlayerIdentifiersByApiKeyLookup.builder()
                .userName("registered-user")
                .systemId("system-id0")
                .ip("127.0.0.1")
                .build()));
  }

  @Test
  void foundCase() {
    final Optional<Integer> result = playerApiKeyDao.lookupPlayerIdByPlayerChatId("chat-id0");

    assertThat(result, isPresentAndIs(50));
  }

  @Test
  void notFoundCase() {
    final Optional<Integer> result = playerApiKeyDao.lookupPlayerIdByPlayerChatId("DNE");

    assertThat(result, isEmpty());
  }
}
