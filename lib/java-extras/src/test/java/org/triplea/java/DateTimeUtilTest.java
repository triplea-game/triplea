package org.triplea.java;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("InnerClassMayBeStatic")
class DateTimeUtilTest {
  private static final Instant NOW = LocalDateTime.of(2020, 1, 1, 14, 30).toInstant(ZoneOffset.UTC);

  @BeforeEach
  void setDefaultZoneToUtcAndFixClock() {
    DateTimeUtil.defaultZoneId = ZoneOffset.UTC;
    DateTimeUtil.clock = Clock.fixed(NOW, ZoneOffset.UTC);
    DateTimeUtil.defaultLocale = Locale.US;
  }

  @Nested
  class LocalizedTime {
    @Test
    void localTimeFrance() {
      DateTimeUtil.defaultLocale = Locale.FRANCE;
      assertThat(DateTimeUtil.getLocalizedTime()).isEqualTo("14:30:00");
    }
  }

  @Test
  void utcInstantOf() {
    assertThat(DateTimeUtil.utcInstantOf(2020, 11, 1, 23, 59))
        .isEqualTo(
            LocalDateTime.of(2020, 11, 1, 23, 59) //
                .toInstant(ZoneOffset.UTC));
  }
}
