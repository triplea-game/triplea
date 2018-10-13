package org.triplea.http.client.throttle.rate;

import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportDetails;

@ExtendWith(MockitoExtension.class)
class RateLimitingThrottleTest {

  private static final ErrorReport ERROR_REPORT = new ErrorReport(ErrorReportDetails.builder()
      .logRecord(new LogRecord(Level.SEVERE, "message"))
      .gameVersion("engine version")
      .build());

  private static final int MIN_MILLIS_BETWEEN_REQUSETS = 5;

  private static final Instant NOW = Instant.now();

  @Mock
  private Supplier<Instant> instantSupplier;


  @Test
  void throttleNumberOfRequestPer() {
    final Consumer<ErrorReport> throttle =
        new RateLimitingThrottle(MIN_MILLIS_BETWEEN_REQUSETS, instantSupplier);

    Mockito.when(instantSupplier.get())
        .thenReturn(
            NOW,
            NOW.plusMillis(MIN_MILLIS_BETWEEN_REQUSETS + 1),
            NOW.plusMillis(MIN_MILLIS_BETWEEN_REQUSETS - 1));

    // no throttle expected on the first call, that should always pass
    throttle.accept(ERROR_REPORT);

    // second call is after the min time, so it should be okay
    throttle.accept(ERROR_REPORT);

    // the third call did not wait long enough, we expect an exception
    Assertions.assertThrows(RateLimitException.class,
        () -> throttle.accept(ERROR_REPORT));
  }
}
