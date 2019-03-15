package org.triplea.http.client;

/**
 * Enumerates the different outcomes that are possible when sending an http request.
 */
public enum SendResult {
  /**
   * Our http request message was sent and we got a 2xx response back.
   */
  SENT,

  /**
   * Indicates we had a server side error on our request, either a timeout or a 5xx.
   */
  SERVER_ERROR,

  /**
   * Server returned a client error response, a 4xx.
   */
  CLIENT_ERROR,

  /**
   * Generic catch-all error if an exception happens that we do not otherwise recognizse.
   */
  COMMUNICATION_ERROR,

  /**
   * Indicates message was not sent, either because it was invalid or was rate limited at the client side.
   */
  NOT_SENT
}
