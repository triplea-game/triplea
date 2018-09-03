package org.triplea.http.client.throttle.rate;

import org.triplea.http.client.throttle.MessageNotSentException;

/**
 * This exceptions represents a client-side throttle when messages are sent too frequently and
 * are blocked client-side from being sent.
 */
public class RateLimitException extends MessageNotSentException {
}
