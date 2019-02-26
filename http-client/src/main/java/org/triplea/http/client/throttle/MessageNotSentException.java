package org.triplea.http.client.throttle;

import lombok.NoArgsConstructor;

/**
 * Raised when message is throttled (not sent and dropped) due to client side restrictions.
 * Such client side throttles are a safety check to guard against bugs that could accidently send bad messages.
 */
@NoArgsConstructor
public class MessageNotSentException extends RuntimeException {
  private static final long serialVersionUID = 3303214733527625189L;

  public MessageNotSentException(final String message) {
    super(message);
  }
}
