package org.triplea.server.moderator.toolbox.api.key.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/** Maps any uncaught {@code ApiKeyVerificationLockOutException} instances to a 403 (forbidden). */
public class ApiKeyLockOutMapper implements ExceptionMapper<ApiKeyLockOutException> {

  @Override
  public Response toResponse(final ApiKeyLockOutException exception) {
    return Response.status(403).entity("Request rejected").build();
  }
}
