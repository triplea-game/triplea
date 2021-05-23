package org.triplea.spitfire.server;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import lombok.Builder;
import lombok.Getter;

/**
 * Creates a custom http Status Code, mainly used to be able to create a custom reason phrase. This
 * is useful if we cannot transmit an http body and need to pass information through the status
 * reason.
 */
@Builder
public class ResponseStatus implements Response.StatusType {
  @Getter(onMethod_ = @Override)
  @Nonnull
  private final String reasonPhrase;

  @Nonnull private final Integer statusCode;

  @Override
  public Family getFamily() {
    return Family.familyOf(getStatusCode());
  }

  @Override
  public int getStatusCode() {
    return statusCode;
  }
}
