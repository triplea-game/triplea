package org.triplea.db.dao.moderator;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.db.TimestampMapper;

/** Return data when querying the user table for moderators. */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ModeratorUserDaoData {
  private String username;
  private Instant lastLogin;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<ModeratorUserDaoData> buildResultMapper() {
    return (rs, ctx) ->
        ModeratorUserDaoData.builder()
            .username(rs.getString("username"))
            .lastLogin(TimestampMapper.map(rs, "access_time"))
            .build();
  }
}
