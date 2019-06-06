package org.triplea.server.moderator.toolbox.api.key.validation.exception;


/**
 * Thrown if a user attempts to guess an API-key. In this case we do not even try to validate the API
 * key and we reject the request.
 */
public class ApiKeyVerificationLockOutException extends RuntimeException {
  private static final long serialVersionUID = -7584274667533893632L;
}
