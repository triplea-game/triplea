package org.triplea.server.moderator.toolbox.api.key.validation.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Maps any uncaught {@code ApiKeyVerificationLockOutException} instances to a 403 (forbidden).
 */
public class ApiKeyVerificationLockOutMapper implements ExceptionMapper<ApiKeyVerificationLockOutException> {

  @Override
  public Response toResponse(final ApiKeyVerificationLockOutException exception) {
    return Response.status(403).entity("Request rejected").build();
  }
}
