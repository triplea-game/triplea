package org.triplea.db.dao.moderator.player.info;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.triplea.http.client.lobby.moderator.PlayerSummary.BanInformation;

/**
 * Represents each row from the ban table where a given system id or IP was banned (within the last
 * N days).
 */
@Getter
public class PlayerBanRecord {
  private final String username;
  private final String ip;
  private final String systemId;
  private final Instant banStart;
  private final Instant banEnd;

  @Builder
  public PlayerBanRecord(
      @ColumnName("name") final String username,
      @ColumnName("ip") final String ip,
      @ColumnName("system_id") final String systemId,
      @ColumnName("ban_start") final Instant banStart,
      @ColumnName("ban_end") final Instant banEnd) {
    this.username = username;
    this.ip = ip;
    this.systemId = systemId;
    this.banStart = banStart;
    this.banEnd = banEnd;
  }

  public BanInformation toBanInformation() {
    return BanInformation.builder()
        .name(username)
        .ip(ip)
        .systemId(systemId)
        .epochMilliStartDate(banStart.toEpochMilli())
        .epochMillEndDate(banEnd.toEpochMilli())
        .build();
  }
}
