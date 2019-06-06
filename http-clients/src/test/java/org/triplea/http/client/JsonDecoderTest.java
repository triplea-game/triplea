package org.triplea.http.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import lombok.AllArgsConstructor;

class JsonDecoderTest {

  @AllArgsConstructor
  private static class InstantExample {
    private final Instant instant;
  }

  /**
   * Test that verifies we can decode an 'Instant' represented as
   * floating point number, in epoch seconds.
   */
  @Test
  void decoder() {
    // Thu Jun 06 2019 04:20:06Z
    final String jsonString = "{\"instant\":1559794806.329342000}";

    final InstantExample event = JsonDecoder.decoder()
        .fromJson(jsonString, InstantExample.class);

    assertThat(event.instant, notNullValue());

    final LocalDateTime dateTime = LocalDateTime.ofInstant(event.instant, ZoneOffset.UTC);
    assertThat(dateTime.getMonth(), is(Month.JUNE));
    assertThat(dateTime.getDayOfMonth(), is(6));
    assertThat(dateTime.getYear(), is(2019));
    assertThat(dateTime.getHour(), is(4));
    assertThat(dateTime.getMinute(), is(20));
  }
}
