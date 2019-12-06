package org.triplea.lobby.server.db.dao.api.key;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import java.net.InetAddress;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.PlayerName;
import org.triplea.domain.data.SystemId;
import org.triplea.java.Postconditions;
import org.triplea.lobby.server.db.dao.UserJdbiDao;
import org.triplea.lobby.server.db.dao.UserRoleDao;
import org.triplea.lobby.server.db.data.UserRole;

/** Wrapper to abstract away DB details of how API key is stored and to provide convenience APIs. */
// TODO: Project#12 rename this back to ApiKeyDaoWrapper
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class LobbyApiKeyDaoWrapper {

  private final LobbyApiKeyDao apiKeyDao;
  private final GameHostingApiKeyDao gameHostApiKeyDao;
  private final UserJdbiDao userJdbiDao;
  private final UserRoleDao userRoleDao;
  private final Supplier<ApiKey> keyMaker;
  /** Hashing function so that we do not store plain-text API key values in database. */
  private final Function<ApiKey, String> keyHashingFunction;

  @SuppressWarnings("UnstableApiUsage")
  public LobbyApiKeyDaoWrapper(final Jdbi jdbi) {
    this(
        jdbi.onDemand(LobbyApiKeyDao.class),
        jdbi.onDemand(GameHostingApiKeyDao.class),
        jdbi.onDemand(UserJdbiDao.class),
        jdbi.onDemand(UserRoleDao.class),
        ApiKey::newKey,
        apiKey -> Hashing.sha512().hashString(apiKey.getValue(), Charsets.UTF_8).toString());
  }

  // TODO: update tests
  public Optional<UserWithRoleRecord> lookupByApiKey(final ApiKey apiKey) {
    return apiKeyDao
        .lookupByApiKey(keyHashingFunction.apply(apiKey))
        .or(
            () ->
                gameHostApiKeyDao.keyExists(keyHashingFunction.apply(apiKey))
                    ? Optional.of(UserWithRoleRecord.builder().role(UserRole.HOST).build())
                    : Optional.empty());
  }

  /** Creates (and stores in DB) a new API key for 'host' connections (AKA: LobbyWatcher). */
  public ApiKey newGameHostKey(final InetAddress ip) {
    Preconditions.checkArgument(ip != null);
    final ApiKey key = keyMaker.get();
    final int insertCount =
        gameHostApiKeyDao.insertKey(keyHashingFunction.apply(key), ip.getHostAddress());
    Postconditions.assertState(insertCount == 1);
    return key;
  }

  /** Creates (and stores in DB) a new API key for registered or anonymous users. */
  public ApiKey newKey(
      final PlayerName playerName,
      final InetAddress ip,
      final SystemId systemId,
      final PlayerChatId playerChatId) {
    Preconditions.checkNotNull(playerName);
    Preconditions.checkNotNull(ip);

    final ApiKey key = keyMaker.get();

    userJdbiDao
        .lookupUserIdAndRoleIdByUserName(playerName.getValue())
        .ifPresentOrElse(
            userRoleLookup -> {
              // insert key for registered user
              insertKey(
                  userRoleLookup.getUserId(),
                  playerName.getValue(),
                  key,
                  ip,
                  systemId,
                  playerChatId,
                  userRoleLookup.getUserRoleId());
            },
            () -> {
              // insert key for anonymous user
              final int anonymousUserRoleId = userRoleDao.lookupRoleId(UserRole.ANONYMOUS);
              insertKey(
                  null,
                  playerName.getValue(),
                  key,
                  ip,
                  systemId,
                  playerChatId,
                  anonymousUserRoleId);
            });
    return key;
  }

  private void insertKey(
      @Nullable final Integer userId,
      @Nullable final String username,
      final ApiKey apiKey,
      final InetAddress ipAddress,
      final SystemId systemId,
      final PlayerChatId playerChatId,
      final int userRoleId) {
    final String hashedKey = keyHashingFunction.apply(apiKey);

    final int rowsInserted =
        apiKeyDao.storeKey(
            username,
            userId,
            userRoleId,
            playerChatId.getValue(),
            hashedKey,
            systemId.getValue(),
            ipAddress.getHostAddress());
    Postconditions.assertState(rowsInserted == 1);
  }

  public Optional<PlayerIdLookup> lookupPlayerByChatId(final PlayerChatId playerChatId) {
    return apiKeyDao.lookupByPlayerChatId(playerChatId.getValue());
  }
}
