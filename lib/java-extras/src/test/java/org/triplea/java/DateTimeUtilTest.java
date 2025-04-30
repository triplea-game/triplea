package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
    private static final String NARROW_NON_BREAKING_SPACE = "\u202F";

    @Test
    void localedTimeUs() {
      DateTimeUtil.defaultLocale = Locale.US;
      assertThat(DateTimeUtil.getLocalizedTime(), is("2:30:00" + NARROW_NON_BREAKING_SPACE + "PM"));
    }

    @Test
    void localedTimeUk() {
      DateTimeUtil.defaultLocale = Locale.ENGLISH;
      assertThat(DateTimeUtil.getLocalizedTime(), is("2:30:00" + NARROW_NON_BREAKING_SPACE + "PM"));
    }

    @Test
    void localedTimeFrance() {
      DateTimeUtil.defaultLocale = Locale.FRANCE;
      assertThat(DateTimeUtil.getLocalizedTime(), is("14:30:00"));
    }
  }

  @Test
  void utcInstantOf() {
    assertThat(
        DateTimeUtil.utcInstantOf(2020, 11, 1, 23, 59),
        is(
            LocalDateTime.of(2020, 11, 1, 23, 59) //
                .toInstant(ZoneOffset.UTC)));
  }
}
