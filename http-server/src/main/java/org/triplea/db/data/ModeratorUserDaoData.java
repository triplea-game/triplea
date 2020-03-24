package org.triplea.db.data;

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
  public static final String USERNAME_COLUMN = "username";
  public static final String LAST_LOGIN_COLUMN = "last_login";

  private String username;
  private Instant lastLogin;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<ModeratorUserDaoData> buildResultMapper() {
    return (rs, ctx) ->
        ModeratorUserDaoData.builder()
            .username(rs.getString(USERNAME_COLUMN))
            .lastLogin(TimestampMapper.map(rs, LAST_LOGIN_COLUMN))
            .build();
  }
}
