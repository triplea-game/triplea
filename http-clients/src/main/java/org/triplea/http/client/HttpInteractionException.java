package org.triplea.http.client;

import feign.FeignException;

/** Exception indicating server returned a non-200 status code or server could not be reached. */
public class HttpInteractionException extends FeignException {
  private static final long serialVersionUID = 2686193677569536322L;

  public HttpInteractionException(final int status, final String message) {
    super(status, message);
  }
}
