package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class DateTimeFormatterUtilTest {
  private static final ZoneId ZONE_ID = ZoneId.ofOffset("UTC", ZoneOffset.of("+08:00"));
  private static final long DEC_FIRST_EPOCH_MILLIS =
      LocalDateTime.of(2000, 12, 1, 23, 59) //
          .toInstant(ZoneOffset.ofHours(8))
          .toEpochMilli();

  @Test
  void verifyFormatting() {
    final String result = //
        DateTimeFormatterUtil.formatEpochMilli(DEC_FIRST_EPOCH_MILLIS, Locale.ENGLISH, ZONE_ID);
    assertThat(result, is("2000-12-1 23:59 (GMT+8)"));
  }
}
