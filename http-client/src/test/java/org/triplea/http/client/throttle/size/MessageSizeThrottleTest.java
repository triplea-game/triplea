package org.triplea.http.client.throttle.size;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.triplea.test.common.Assertions.assertNotThrows;

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
        () -> new MessageSizeThrottle(characterLimit)
            .accept(ERROR_REPORT));
  }

  @SuppressWarnings("JUnitTestMethodWithNoAssertions")
  @Test
  void messageSizeUnderLimitIsAllowed() {
    final int characterLimit = ERROR_REPORT.toString().length() + 1;

    assertNotThrows(() -> new MessageSizeThrottle(characterLimit)
        .accept(ERROR_REPORT));
  }
}
