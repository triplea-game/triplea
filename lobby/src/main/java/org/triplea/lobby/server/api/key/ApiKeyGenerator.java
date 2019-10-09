package org.triplea.lobby.server.api.key;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;
import org.triplea.lobby.server.db.dao.ApiKeyDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@Builder
public class ApiKeyGenerator implements Function<PlayerName, ApiKey> {

  @Nonnull private final ApiKeyDao apiKeyDao;
  @Nonnull private final UserJdbiDao userDao;
  @Nonnull private final Supplier<ApiKey> keyMaker;

  /**
   * Generates a random string value that is extremely difficult to guess or crack. <br>
   * This key generator is intended to have a pretty fast runtime. <br>
   * <br>
   * To generate:<br>
   * (1) We generate a UUID.<br>
   * (2) The UUID is hashed to prevent guessing the next value. This makes it difficult to crack the
   * seed value being used.<br>
   * (3) Finally, we attach nano epoch to make the API key even more difficult to guess. <br>
   */
  public static Supplier<ApiKey> createKeyMaker() {
    return () -> ApiKey.of(randomHash() + System.nanoTime());
  }

  private static String randomHash() {
    return Hashing.sha512()
        .hashString(UUID.randomUUID().toString(), StandardCharsets.UTF_8)
        .toString();
  }

  @Override
  public ApiKey apply(final PlayerName playerName) {
    final ApiKey key = keyMaker.get();

    final Integer userId = userDao.lookupUserIdByName(playerName.getValue()).orElse(null);
    apiKeyDao.storeKey(userId, key.getValue());
    apiKeyDao.deleteOldKeys();
    return key;
  }
}
