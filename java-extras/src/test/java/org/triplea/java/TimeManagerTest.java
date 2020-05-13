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
class TimeManagerTest {
  private static final Instant NOW = LocalDateTime.of(2020, 1, 1, 14, 30).toInstant(ZoneOffset.UTC);

  @BeforeEach
  void setDefaultZoneToUtcAndFixClock() {
    TimeManager.defaultZoneId = ZoneOffset.UTC;
    TimeManager.clock = Clock.fixed(NOW, ZoneOffset.UTC);
    TimeManager.defaultLocale = Locale.US;
  }

  @Nested
  class LocalizedTime {
    @Test
    void localedTimeUs() {
      TimeManager.defaultLocale = Locale.US;
      assertThat(TimeManager.getLocalizedTime(), is("2:30:00 PM"));
    }

    @Test
    void localedTimeUk() {
      TimeManager.defaultLocale = Locale.ENGLISH;
      assertThat(TimeManager.getLocalizedTime(), is("2:30:00 PM"));
    }

    @Test
    void localedTimeFrance() {
      TimeManager.defaultLocale = Locale.FRANCE;
      assertThat(TimeManager.getLocalizedTime(), is("14:30:00"));
    }
  }

  @Test
  void toDateString() {
    assertThat(
        TimeManager.toDateString(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)),
        is("Wed. Jan. 01 14:30:00 Z 2020"));
  }

  @Test
  void utcInstantOf() {
    assertThat(
        TimeManager.utcInstantOf(2020, 11, 1, 23, 59),
        is(
            LocalDateTime.of(2020, 11, 1, 23, 59) //
                .toInstant(ZoneOffset.UTC)));
  }
}
