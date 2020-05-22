package org.triplea.db.dao.moderator.player.info;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.db.TimestampMapper;
import org.triplea.http.client.lobby.moderator.PlayerSummary.Alias;

/**
 * Represents all distinct matching rows (within the last N days) in the access log table with a
 * matching IP or system ID. This should tell us each name, or aliases, that was presumably used by
 * a given player.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PlayerAliasRecord {

  private String username;
  private String ip;
  private String systemId;
  private Instant date;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<PlayerAliasRecord> buildResultMapper() {
    return (rs, ctx) ->
        PlayerAliasRecord.builder()
            .username(rs.getString("name"))
            .ip(rs.getString("ip"))
            .systemId(rs.getString("systemId"))
            .date(TimestampMapper.map(rs, "accessTime"))
            .build();
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
