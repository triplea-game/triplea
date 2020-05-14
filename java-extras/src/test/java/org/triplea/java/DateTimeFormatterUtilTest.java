package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.triplea.java.DateTimeFormatterUtil.FormatOption;

class DateTimeFormatterUtilTest {
  private static final Instant JAN_FIRST_INSTANT =
      LocalDateTime.of(2020, 1, 1, 14, 30).toInstant(ZoneOffset.UTC);

  private static final long DEC_FIRST_EPOCH_MILLIS =
      LocalDateTime.of(2000, 12, 1, 23, 59) //
          .toInstant(ZoneOffset.ofHours(8))
          .toEpochMilli();

  @BeforeAll
  static void setDefaultToUtc() {
    DateTimeFormatterUtil.setDefaultToUtc();
  }

  @Test
  void verifyFormattingWithTimeZone() {
    final String result =
        DateTimeFormatterUtil.formatInstant(Instant.ofEpochMilli(DEC_FIRST_EPOCH_MILLIS));
    assertThat(result, is("2000-12-1 15:59 (GMT)"));
  }

  @Test
  void verifyFormattingNoTimeZone() {
    final String result =
        DateTimeFormatterUtil.formatEpochMilli(
            DEC_FIRST_EPOCH_MILLIS, FormatOption.WITHOUT_TIMEZONE);
    assertThat(result, is("2000-12-1 15:59"));
  }

  @Test
  void toDateString() {
    assertThat(
        DateTimeFormatterUtil.toDateString(
            LocalDateTime.ofInstant(JAN_FIRST_INSTANT, ZoneOffset.UTC)),
        is("Wed Jan. 01 14:30:00 UTC 2020"));
  }
}
