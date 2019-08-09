package org.triplea.server.moderator.toolbox.api.key.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/** Maps any uncaught {@code IncorrectApiKeyException} instances to a 401 (not authorized). */
public class IncorrectApiKeyMapper implements ExceptionMapper<IncorrectApiKeyException> {

  @Override
  public Response toResponse(final IncorrectApiKeyException exception) {
    return Response.status(401).entity("Invalid API key").build();
  }
}
