package org.triplea.db.dao.moderator.player.info;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.triplea.http.client.lobby.moderator.PlayerSummary.Alias;

/**
 * Represents all distinct matching rows (within the last N days) in the access log table with a
 * matching IP or system ID. This should tell us each name, or aliases, that was presumably used by
 * a given player.
 */
@Getter
public class PlayerAliasRecord {
  private final String username;
  private final String ip;
  private final String systemId;
  private final Instant date;

  @Builder
  public PlayerAliasRecord(
      @ColumnName("name") final String username,
      @ColumnName("ip") final String ip,
      @ColumnName("systemId") final String systemId,
      @ColumnName("accessTime") final Instant accessTime) {
    this.username = username;
    this.ip = ip;
    this.systemId = systemId;
    this.date = accessTime;
  }

  public Alias toAlias() {
    return Alias.builder()
        .name(username)
        .ip(ip)
        .systemId(systemId)
        .epochMilliDate(date.toEpochMilli())
        .build();
  }
}
