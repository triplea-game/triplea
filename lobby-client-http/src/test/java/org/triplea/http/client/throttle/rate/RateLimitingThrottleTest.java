package org.triplea.http.client.throttle.rate;

import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.error.report.ErrorUploadRequest;

@ExtendWith(MockitoExtension.class)
class RateLimitingThrottleTest {

  private static final ErrorUploadRequest ERROR_REPORT = ErrorUploadRequest.builder()
      .title("Hunger is a scurvy tuna.")
      .body("Woodchucks are the sails of the scrawny desolation.")
      .build();

  private static final int MIN_MILLIS_BETWEEN_REQUSETS = 5;

  private static final Instant NOW = Instant.now();

  @Mock
  private Supplier<Instant> instantSupplier;


  @Test
  void throttleNumberOfRequestPer() {
    final Consumer<ErrorUploadRequest> throttle =
        new RateLimitingThrottle<>(MIN_MILLIS_BETWEEN_REQUSETS, instantSupplier);

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
