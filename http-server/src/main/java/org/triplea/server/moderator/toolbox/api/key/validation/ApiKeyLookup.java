package org.triplea.server.moderator.toolbox.api.key.validation;

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.triplea.lobby.server.db.ApiKeyDao;

import lombok.Builder;

/**
 * This class does a lookup against database to validate a moderators API key. If valid
 * then we'll return the moderators database ID.
 */
@Builder
class ApiKeyLookup implements Function<String, Optional<Integer>> {

  @Nonnull
  private final ApiKeyDao apiKeyDao;
  @Nonnull
  private final Function<String, String> hashingFunction;

  @Override
  public Optional<Integer> apply(final String apiKey) {
    return apiKeyDao.lookupModeratorIdByApiKey(
        hashingFunction.apply(apiKey));
  }
}
