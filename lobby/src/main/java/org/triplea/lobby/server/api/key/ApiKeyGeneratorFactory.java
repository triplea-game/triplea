package org.triplea.lobby.server.api.key;

import games.strategy.engine.lobby.ApiKey;
import games.strategy.engine.lobby.PlayerName;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiKeyGeneratorFactory {

  public static Function<PlayerName, ApiKey> newApiKeyGenerator(final Jdbi jdbi) {
    return ApiKeyGenerator.builder()
        .keyMaker(ApiKeyGenerator.createKeyMaker())
        .apiKeyDao(
            // TODO: Project#12 - use a real ApiKeyDao implementation
            (key, isAdmin) -> {})
        .build();
  }
}
