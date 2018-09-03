package org.triplea.http.client.throttle.rate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

import org.triplea.http.data.error.report.ErrorReport;

import com.google.common.annotations.VisibleForTesting;

/**
 * A simple rate limit throttle that remembers the time of last request
 * and rejects new requests if not enough time has elapsed since
 * the previous request.
 */
public class RateLimitingThrottle implements Consumer<ErrorReport> {

  private static final int MIN_MILLIS_BETWEEN_REQUESTS = 15_000;
  private final int minMillisBetweenRequests;
  private volatile Instant lastInstant = Instant.ofEpochMilli(0);

  public RateLimitingThrottle() {
    this(MIN_MILLIS_BETWEEN_REQUESTS);
  }

  @VisibleForTesting
  RateLimitingThrottle(final int minMillisBetweenRequests) {
    this.minMillisBetweenRequests = minMillisBetweenRequests;
  }

  @Override
  public void accept(final ErrorReport errorReport) {
    final Instant nextInstant = Instant.now();

    final long millisSinceLast;
    millisSinceLast = lastInstant.until(nextInstant, ChronoUnit.MILLIS);
    lastInstant = nextInstant;

    if (millisSinceLast < minMillisBetweenRequests) {
      throw new RateLimitException();
    }
  }
}
