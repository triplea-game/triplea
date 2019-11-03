package org.triplea.lobby.server.db.dao.access.log;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.lobby.server.db.TimestampMapper;

/** Return data when selecting lobby access history. */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AccessLogRecord {
  public static final String ACCESS_TIME_COLUMN = "access_time";
  public static final String USERNAME_COLUMN = "username";
  public static final String IP_COLUMN = "ip";
  public static final String SYSTEM_ID_COLUMN = "system_id";
  public static final String REGISTERED_COLUMN = "registered";

  private Instant accessTime;
  private String username;
  private String ip;
  private String systemId;
  private boolean registered;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<AccessLogRecord> buildResultMapper() {
    return (rs, ctx) ->
        AccessLogRecord.builder()
            .accessTime(TimestampMapper.map(rs, ACCESS_TIME_COLUMN))
            .username(rs.getString(USERNAME_COLUMN))
            .ip(rs.getString(IP_COLUMN))
            .systemId(rs.getString(SYSTEM_ID_COLUMN))
            .registered(rs.getBoolean(REGISTERED_COLUMN))
            .build();
  }
}
