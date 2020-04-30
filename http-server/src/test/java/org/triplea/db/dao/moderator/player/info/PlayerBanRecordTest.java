package org.triplea.db.dao.moderator.player.info;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator.BanInformation;

class PlayerBanRecordTest {
  private static final Instant START_DATE =
      LocalDateTime.of(1990, 1, 1, 23, 59, 59) //
          .toInstant(ZoneOffset.UTC);
  private static final Instant END_DATE =
      LocalDateTime.of(2010, 1, 1, 23, 59, 59) //
          .toInstant(ZoneOffset.UTC);

  @Test
  void testToBanInformation() {
    final BanInformation banInformation =
        PlayerBanRecord.builder()
            .banStart(START_DATE)
            .banEnd(END_DATE)
            .ip("1.1.1.1")
            .systemId("system-id")
            .username("name")
            .build()
            .toBanInformation();

    assertThat(banInformation.getEpochMilliStartDate(), is(START_DATE.toEpochMilli()));
    assertThat(banInformation.getEpochMillEndDate(), is(END_DATE.toEpochMilli()));
    assertThat(banInformation.getIp(), is("1.1.1.1"));
    assertThat(banInformation.getName(), is("name"));
  }
}
