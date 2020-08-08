package org.triplea.db.dao.access.log;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.db.TimestampMapper;

/** Return data when selecting lobby access history. */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AccessLogRecord {
  private Instant accessTime;
  private String username;
  private String ip;
  private String systemId;
  private boolean registered;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<AccessLogRecord> buildResultMapper() {
    return (rs, ctx) ->
        AccessLogRecord.builder()
            .accessTime(TimestampMapper.map(rs, "access_time"))
            .username(rs.getString("username"))
            .ip(rs.getString("ip"))
            .systemId(rs.getString("system_id"))
            .registered(rs.getBoolean("registered"))
            .build();
  }
}
