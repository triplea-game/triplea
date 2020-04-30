package org.triplea.db.dao.moderator.player.info;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.db.TimestampMapper;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator.BanInformation;

/**
 * Represents each row from the ban table where a given system id or IP was banned (within the last
 * N days).
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PlayerBanRecord {

  private String username;
  private String ip;
  private String systemId;
  private Instant banStart;
  private Instant banEnd;

  public static RowMapper<PlayerBanRecord> buildResultMapper() {
    return (rs, ctx) ->
        PlayerBanRecord.builder()
            .username(rs.getString("name"))
            .ip(rs.getString("ip"))
            .systemId(rs.getString("systemId"))
            .banStart(TimestampMapper.map(rs, "banStart"))
            .banEnd(TimestampMapper.map(rs, "banEnd"))
            .build();
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
