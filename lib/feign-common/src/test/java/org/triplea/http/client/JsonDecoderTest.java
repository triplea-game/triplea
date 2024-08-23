package org.triplea.http.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Test;

class JsonDecoderTest {
  // Thu Jun 06 2019 04:20:06Z
  @NonNls private static final String JSON_STRING = "{\"instant\":1559794806.329342000}";

  @AllArgsConstructor
  private static class InstantExample {
    private final Instant instant;
  }

  /**
   * Test that verifies we can decode an 'Instant' represented as floating point number, in epoch
   * seconds.
   */
  @Test
  void decoder() {
    final InstantExample event = JsonDecoder.decoder().fromJson(JSON_STRING, InstantExample.class);

    assertThat(event.instant, notNullValue());

    final LocalDateTime dateTime = LocalDateTime.ofInstant(event.instant, ZoneOffset.UTC);
    assertThat(dateTime.getMonth(), is(Month.JUNE));
    assertThat(dateTime.getDayOfMonth(), is(6));
    assertThat(dateTime.getYear(), is(2019));
    assertThat(dateTime.getHour(), is(4));
    assertThat(dateTime.getMinute(), is(20));
  }

  @Test
  void verifyEpochSecondAndNanoDecoding() {
    final InstantExample event = JsonDecoder.decoder().fromJson(JSON_STRING, InstantExample.class);

    assertThat(event.instant, is(Instant.ofEpochSecond(1559794806, 329342000)));
  }
}
