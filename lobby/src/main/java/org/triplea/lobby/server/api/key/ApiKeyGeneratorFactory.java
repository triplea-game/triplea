package org.triplea.lobby.server.api.key;

import java.net.InetAddress;
import java.util.function.BiFunction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;
import org.triplea.lobby.server.db.dao.api.key.LobbyApiKeyDaoWrapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiKeyGeneratorFactory {

  /** Creates a new key generator instance that can generate a new API key for a given player. */
  public static BiFunction<PlayerName, InetAddress, ApiKey> newApiKeyGenerator(final Jdbi jdbi) {
    final LobbyApiKeyDaoWrapper wrapper = new LobbyApiKeyDaoWrapper(jdbi);
    return wrapper::newKey;
  }
}
