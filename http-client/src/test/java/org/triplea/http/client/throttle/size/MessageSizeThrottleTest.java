package org.triplea.http.client.throttle.size;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportDetails;

class MessageSizeThrottleTest {

  private static final ErrorReport ERROR_REPORT = new ErrorReport(ErrorReportDetails.builder()
      .gameVersion("version info")
      .build());

  @Test
  void messageSizeOverLimitIsNotAllowed() {
    final int characterLimit = ERROR_REPORT.toString().length() - 1;

    assertThrows(MessageExceedsMaxSizeException.class,
        () -> new MessageSizeThrottle<>(characterLimit)
            .accept(ERROR_REPORT));
  }

  @Test
  void messageSizeUnderLimitIsAllowed() {
    final int characterLimit = ERROR_REPORT.toString().length() + 1;

    assertDoesNotThrow(() -> new MessageSizeThrottle<>(characterLimit)
        .accept(ERROR_REPORT));
  }
}
