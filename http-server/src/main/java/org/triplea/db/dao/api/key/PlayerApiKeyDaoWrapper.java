package org.triplea.db.dao.api.key;

import com.google.common.base.Preconditions;
import java.net.InetAddress;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.user.UserJdbiDao;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.db.dao.user.role.UserRoleDao;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;
import org.triplea.java.Postconditions;

/** Wrapper to abstract away DB details of how API key is stored and to provide convenience APIs. */
@Builder
public class PlayerApiKeyDaoWrapper {

  @Nonnull private final PlayerApiKeyDao lobbyApiKeyDao;
  @Nonnull private final UserJdbiDao userJdbiDao;
  @Nonnull private final UserRoleDao userRoleDao;
  @Nonnull private final Supplier<ApiKey> keyMaker;
  /** Hashing function so that we do not store plain-text API key values in database. */
  @Nonnull private final Function<ApiKey, String> keyHashingFunction;

  public static PlayerApiKeyDaoWrapper build(final Jdbi jdbi) {
    return PlayerApiKeyDaoWrapper.builder()
        .lobbyApiKeyDao(jdbi.onDemand(PlayerApiKeyDao.class))
        .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
        .userRoleDao(jdbi.onDemand(UserRoleDao.class))
        .keyMaker(ApiKey::newKey)
        .keyHashingFunction(new ApiKeyHasher())
        .build();
  }

  public Optional<PlayerApiKeyLookupRecord> lookupByApiKey(final ApiKey apiKey) {
    return lobbyApiKeyDao.lookupByApiKey(keyHashingFunction.apply(apiKey));
  }

  /** Creates (and stores in DB) a new API key for registered or anonymous users. */
  public ApiKey newKey(
      final UserName userName,
      final InetAddress ip,
      final SystemId systemId,
      final PlayerChatId playerChatId) {
    Preconditions.checkNotNull(userName);
    Preconditions.checkNotNull(ip);

    final ApiKey key = keyMaker.get();

    userJdbiDao
        .lookupUserIdAndRoleIdByUserName(userName.getValue())
        .ifPresentOrElse(
            userRoleLookup -> {
              // insert key for registered user
              insertKey(
                  userRoleLookup.getUserId(),
                  userName.getValue(),
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
                  null, userName.getValue(), key, ip, systemId, playerChatId, anonymousUserRoleId);
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
        lobbyApiKeyDao.storeKey(
            username,
            userId,
            userRoleId,
            playerChatId.getValue(),
            hashedKey,
            systemId.getValue(),
            ipAddress.getHostAddress());
    Postconditions.assertState(rowsInserted == 1);
  }

  public Optional<PlayerIdentifiersByApiKeyLookup> lookupPlayerByChatId(
      final PlayerChatId playerChatId) {
    return lobbyApiKeyDao.lookupByPlayerChatId(playerChatId.getValue());
  }
}
