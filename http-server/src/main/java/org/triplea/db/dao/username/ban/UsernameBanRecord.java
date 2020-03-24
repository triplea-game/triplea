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

  public static final String USERNAME_COLUMN = "username";
  public static final String DATE_CREATED_COLUMN = "date_created";

  private String username;
  private Instant dateCreated;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<UsernameBanRecord> buildResultMapper() {
    return (rs, ctx) ->
        UsernameBanRecord.builder()
            .username(rs.getString(USERNAME_COLUMN))
            .dateCreated(TimestampMapper.map(rs, DATE_CREATED_COLUMN))
            .build();
  }
}
