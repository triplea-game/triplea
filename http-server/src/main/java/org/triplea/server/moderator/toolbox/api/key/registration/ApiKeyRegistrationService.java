package org.triplea.server.moderator.toolbox.api.key.registration;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import org.triplea.lobby.server.db.ApiKeyRegistrationDao;
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
  private final Function<String, String> singleKeyHasher;

  private final BiFunction<String, String, String> keyHasher;

  private final Supplier<String> newApiKeySupplier;

  private final InvalidKeyLockOut invalidKeyLockOut;

  private final ApiKeyRegistrationDao apiKeyRegistrationDao;

  String registerKey(
      final HttpServletRequest request, final String singleUseKey, final String newPassword) {
    Preconditions.checkNotNull(singleUseKey);
    Preconditions.checkNotNull(newPassword);

    if (invalidKeyLockOut.isLockedOut(request)) {
      throw new ApiKeyLockOutException();
    }

    final String hashedKey = singleKeyHasher.apply(singleUseKey);
    final int userId = apiKeyRegistrationDao.lookupModeratorBySingleUseKey(hashedKey)
        .orElseThrow(() -> {
          log.warning("API key registration failed, incorrect key. Attempted by host: " + request.getRemoteHost());
          return new IncorrectApiKeyException();
        });

    final String newKey = newApiKeySupplier.get();
    final String hashedNewKey = keyHasher.apply(newKey, newPassword);

    apiKeyRegistrationDao.invalidateOldKeyAndInsertNew(
        userId, hashedKey, hashedNewKey);

    return newKey;
  }
}
