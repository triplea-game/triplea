package org.triplea.http.client.throttle.size;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.triplea.http.data.error.report.ErrorReport;
import org.triplea.http.data.error.report.ErrorReportDetails;

class MessageSizeThrottleTest {

  private static final ErrorReport ERROR_REPORT = new ErrorReport(ErrorReportDetails.builder()
      .gameVersion("version info")
      .build());

  @Test
  void messageSizeOverLimitIsNotAllowed() {
    final int characterLimit = ERROR_REPORT.toString().length() - 1;

    Assertions.assertThrows(MessageExceedsMaxSizeException.class,
        () -> new MessageSizeThrottle(characterLimit)
            .accept(ERROR_REPORT));
  }

  @SuppressWarnings("JUnitTestMethodWithNoAssertions")
  @Test
  void messageSizeUnderLimitIsAllowed() {
    final int characterLimit = ERROR_REPORT.toString().length() + 1;

    new MessageSizeThrottle(characterLimit)
        .accept(ERROR_REPORT);
    // no exceptions expected
  }
}
