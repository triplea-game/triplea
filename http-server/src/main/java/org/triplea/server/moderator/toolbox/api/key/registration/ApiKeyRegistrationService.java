package org.triplea.server.moderator.toolbox.api.key.registration;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import org.triplea.lobby.server.db.dao.ModeratorApiKeyDao;
import org.triplea.lobby.server.db.dao.ModeratorKeyRegistrationDao;
import org.triplea.lobby.server.db.dao.ModeratorSingleUseKeyDao;
import org.triplea.server.moderator.toolbox.api.key.InvalidKeyLockOut;
import org.triplea.server.moderator.toolbox.api.key.exception.ApiKeyLockOutException;
import org.triplea.server.moderator.toolbox.api.key.exception.IncorrectApiKeyException;

import com.google.common.base.Preconditions;

import lombok.Builder;
import lombok.extern.java.Log;

/**
 * This service is used to consume a "single-use-key" and generate a new key.
 * It is used by moderators to obtain a new permanent key that is only known to them.
 * The single-use-key is issues to moderators and is rendered invalid after registration.
 * The new key is salted with a password provided by the moderator and stored as a hash.
 * The unsalted new key is returned to the moderator and is otherwise not stored
 * anywhere else beyond the moderators OS.
 */
@Builder
@Log
public class ApiKeyRegistrationService {
  @Nonnull
  private final Function<String, String> singleKeyHasher;

  @Nonnull
  private final BiFunction<String, String, String> keyHasher;

  @Nonnull
  private final Supplier<String> newApiKeySupplier;

  @Nonnull
  private final InvalidKeyLockOut invalidKeyLockOut;

  @Nonnull
  private final ModeratorApiKeyDao moderatorApiKeyDao;

  @Nonnull
  private final ModeratorSingleUseKeyDao moderatorSingleUseKeyDao;

  @Nonnull
  private final ModeratorKeyRegistrationDao moderatorKeyRegistrationDao;

  @Nonnull
  private Predicate<String> apiKeyPasswordBlacklist;

  /**
   * Method to validate a given single-use-key, if valid then we generate a new API key, salt it with
   * password, store the salted password as a hashed value in DB and return to the front-end the new API key.
   *
   * @throws ApiKeyLockOutException Thrown if the requestor has too many failed attempts, no validation
   *         was attempted, the request is rejected.
   * @throws IncorrectApiKeyException Thrown if the single-use key provided is not found in database.
   * @throws PasswordTooEasyException Thrown if the requested password is on the password blacklist.
   */
  String registerKey(
      final HttpServletRequest request, final String singleUseKey, final String newPassword) {
    Preconditions.checkNotNull(singleUseKey);
    Preconditions.checkNotNull(newPassword);

    if (invalidKeyLockOut.isLockedOut(request)) {
      throw new ApiKeyLockOutException();
    }

    if (apiKeyPasswordBlacklist.test(newPassword)) {
      throw new PasswordTooEasyException();
    }

    final String hashedKey = singleKeyHasher.apply(singleUseKey);
    final int moderatorId = moderatorSingleUseKeyDao.lookupModeratorBySingleUseKey(hashedKey)
        .orElseThrow(() -> {
          log.warning("API key registration failed, incorrect key. Attempted by host: " + request.getRemoteHost());
          return new IncorrectApiKeyException();
        });

    final String newKey = newApiKeySupplier.get();
    final String hashedNewKey = keyHasher.apply(newKey, newPassword);

    moderatorKeyRegistrationDao.invalidateSingleUseKeyAndGenerateNew(
        ModeratorKeyRegistrationDao.Params.builder()
            .registeringMachineIp(request.getRemoteAddr())
            .apiKeyDao(moderatorApiKeyDao)
            .singleUseKeyDao(moderatorSingleUseKeyDao)
            .newKey(hashedNewKey)
            .singleUseKey(hashedKey)
            .userId(moderatorId)
            .build());
    return newKey;
  }
}
