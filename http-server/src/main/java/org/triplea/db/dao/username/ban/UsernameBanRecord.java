package org.triplea.db.dao.username.ban;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.db.TimestampMapper;

/** Return data when querying the banned username table. */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UsernameBanRecord {
  private String username;
  private Instant dateCreated;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<UsernameBanRecord> buildResultMapper() {
    return (rs, ctx) ->
        UsernameBanRecord.builder()
            .username(rs.getString("username"))
            .dateCreated(TimestampMapper.map(rs, "date_created"))
            .build();
  }
}
