package org.triplea.http.client.error.report.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ErrorReportDetailsTest {


  private ErrorReportDetails.ErrorReportDetailsBuilder minMessage;

  @BeforeEach
  void setup() {
    minMessage = ErrorReportDetails.builder()
        .gameVersion("game version");
  }

  @Test
  void logRecordNullableConvertedToAnOptional() {
    assertThat(
        minMessage.build()
            .getLogRecord()
            .isPresent(),
        is(false));

    assertThat(
        minMessage
            .logRecord(new LogRecord(Level.SEVERE, "msg"))
            .build()
            .getLogRecord()
            .isPresent(),
        is(true));
  }
}
