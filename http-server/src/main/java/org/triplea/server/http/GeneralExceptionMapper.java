package org.triplea.server.http;

import com.google.common.base.Joiner;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/** Sends exception details to client, should be used in non-prod only */
class GeneralExceptionMapper implements ExceptionMapper<Exception> {
  @Override
  public Response toResponse(final Exception exception) {
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(
            "Http 400 - Bad Request: "
                + exception.getMessage()
                + " , "
                + Joiner.on("\n").join(exception.getStackTrace()))
        .build();
  }
}
