package org.triplea.server.moderator.toolbox.api.key;

import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.triplea.lobby.server.db.dao.ModeratorSingleUseKeyDao;
import org.triplea.lobby.server.db.dao.UserLookupDao;

import com.google.common.base.Preconditions;

import lombok.Builder;

/**
 * Service class that contains business logic for creating a moderators single-use key.
 * The single use key is a value that we can give to a moderator, or a moderator can generate for
 * themselves, and because it is only valid for one use it's okay if it's compromised by email it
 * to yourself or if others know it. Once a single-use key is registered, or used, then a new
 * key is created which then should be kept secure.
 */
@Builder
public class GenerateSingleUseKeyService {
  @Nonnull
  private final Function<String, String> singleUseKeyHasher;
  @Nonnull
  private final ModeratorSingleUseKeyDao singleUseKeyDao;
  @Nonnull
  private final UserLookupDao userLookupDao;
  @Nonnull
  private final Supplier<String> keySupplier;

  public String generateSingleUseKey(final String moderatorName) {
    final Integer userId = userLookupDao.lookupUserIdByName(moderatorName)
        .orElseThrow(
            () -> new IllegalStateException("No user found by name: " + moderatorName));
    return generateSingleUseKey(userId);
  }

  String generateSingleUseKey(final int moderatorId) {
    final String key = keySupplier.get();
    Preconditions.checkState(
        singleUseKeyDao.insertSingleUseKey(moderatorId, singleUseKeyHasher.apply(key)) == 1);
    return key;
  }
}
