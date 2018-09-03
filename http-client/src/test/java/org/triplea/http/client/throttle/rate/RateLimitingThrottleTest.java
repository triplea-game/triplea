package org.triplea.http.client.throttle.rate;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.triplea.http.data.error.report.ErrorReport;
import org.triplea.http.data.error.report.ErrorReportDetails;

class RateLimitingThrottleTest {

  private static final ErrorReport ERROR_REPORT = new ErrorReport(ErrorReportDetails.builder()
      .logRecord(new LogRecord(Level.SEVERE, "message"))
      .gameVersion("engine version")
      .build());

  private static final int MIN_MILLIS_BETWEEN_REQUSETS = 5;


  @Test
  void throttleNumberOfRequestPer() throws Exception {
    final Consumer<ErrorReport> throttle =
        new RateLimitingThrottle(MIN_MILLIS_BETWEEN_REQUSETS);

    // no throttle expected on the first call
    throttle.accept(ERROR_REPORT);

    Thread.sleep(MIN_MILLIS_BETWEEN_REQUSETS + 1);

    // no thottle expected given we had the wait period
    throttle.accept(ERROR_REPORT);

    Thread.sleep(MIN_MILLIS_BETWEEN_REQUSETS + 1);

    // ensure the n+1 works if we wait longer than the min time
    throttle.accept(ERROR_REPORT);

    // now trigger the throttle by sending another request without any delay
    Assertions.assertThrows(RateLimitException.class,
        () -> throttle.accept(ERROR_REPORT));
  }
}
