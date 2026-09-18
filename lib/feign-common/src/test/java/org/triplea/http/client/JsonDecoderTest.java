package org.triplea.http.client;

import static org.assertj.core.api.Assertions.assertThat;

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

    assertThat(event.instant).isNotNull();

    final LocalDateTime dateTime = LocalDateTime.ofInstant(event.instant, ZoneOffset.UTC);
    assertThat(dateTime.getMonth()).isEqualTo(Month.JUNE);
    assertThat(dateTime.getDayOfMonth()).isEqualTo(6);
    assertThat(dateTime.getYear()).isEqualTo(2019);
    assertThat(dateTime.getHour()).isEqualTo(4);
    assertThat(dateTime.getMinute()).isEqualTo(20);
  }

  @Test
  void verifyEpochSecondAndNanoDecoding() {
    final InstantExample event = JsonDecoder.decoder().fromJson(JSON_STRING, InstantExample.class);

    assertThat(event.instant).isEqualTo(Instant.ofEpochSecond(1_559_794_806, 329_342_000));
  }
}
