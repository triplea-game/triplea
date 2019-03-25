package org.triplea.http.client;

import feign.FeignException;

class HttpCommunicationException extends FeignException {
  private static final long serialVersionUID = 2686193677569536322L;

  HttpCommunicationException(final int status, final String message) {
    super(status, message);
  }
}
