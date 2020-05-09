package org.triplea.db.dao.user.ban;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.db.TimestampMapper;

/**
 * Return data when querying about user bans. The public ban id is for public reference, this is a
 * value we can show to banned users so they can report which ban is impacting them. With that
 * information we have the ability to remove the ban without needing to ask them for mac or IP.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserBanRecord {

  private String publicBanId;
  private String username;
  private String systemId;
  private String ip;
  private Instant banExpiry;
  private Instant dateCreated;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<UserBanRecord> buildResultMapper() {
    return (rs, ctx) ->
        UserBanRecord.builder()
            .publicBanId(rs.getString("public_id"))
            .username(rs.getString("username"))
            .systemId(rs.getString("system_id"))
            .ip(rs.getString("ip"))
            .dateCreated(TimestampMapper.map(rs, "date_created"))
            .banExpiry(TimestampMapper.map(rs, "ban_expiry"))
            .build();
  }
}
