package org.triplea.db.dao.moderator.player.info;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator.Alias;

class PlayerAliasRecordTest {
  private static final Instant DATE =
      LocalDateTime.of(1990, 1, 1, 23, 59, 59) //
          .toInstant(ZoneOffset.UTC);

  @Test
  void toAlias() {
    final Alias alias =
        PlayerAliasRecord.builder()
            .date(DATE)
            .ip("1.1.1.1")
            .systemId("system-id")
            .username("name")
            .build()
            .toAlias();

    assertThat(alias.getEpochMilliDate(), is(DATE.toEpochMilli()));
    assertThat(alias.getIp(), is("1.1.1.1"));
    assertThat(alias.getName(), is("name"));
    assertThat(alias.getSystemId(), is("system-id"));
  }
}
