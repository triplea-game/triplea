package org.triplea.db.dao.api.key;

import com.google.common.base.Preconditions;
import java.net.InetAddress;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.domain.data.ApiKey;
import org.triplea.java.Postconditions;

/** Wrapper to abstract away DB details of how API key is stored and to provide convenience APIs. */
@Builder
public class GameHostingApiKeyDaoWrapper {

  @Nonnull private final GameHostingApiKeyDao gameHostApiKeyDao;
  @Nonnull private final Supplier<ApiKey> keyMaker;
  /** Hashing function so that we do not store plain-text API key values in database. */
  @Nonnull private final Function<ApiKey, String> keyHashingFunction;

  public static GameHostingApiKeyDaoWrapper build(final Jdbi jdbi) {
    return GameHostingApiKeyDaoWrapper.builder()
        .gameHostApiKeyDao(jdbi.onDemand(GameHostingApiKeyDao.class))
        .keyMaker(ApiKey::newKey)
        .keyHashingFunction(new ApiKeyHasher())
        .build();
  }

  public boolean isKeyValid(final ApiKey apiKey) {
    return gameHostApiKeyDao.keyExists(keyHashingFunction.apply(apiKey));
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
}
