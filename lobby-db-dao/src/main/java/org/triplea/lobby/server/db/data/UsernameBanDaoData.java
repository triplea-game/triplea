package org.triplea.lobby.server.db.data;

import java.time.Instant;

import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.lobby.server.db.TimestampMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Return data when querying the banned username table.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UsernameBanDaoData {

  public static final String USERNAME_COLUMN = "username";
  public static final String DATE_CREATED_COLUMN = "date_created";

  private String username;
  private Instant dateCreated;

  /**
   * Returns a JDBI row mapper used to convert results into an instance of this bean object.
   */
  public static RowMapper<UsernameBanDaoData> buildResultMapper() {
    return (rs, ctx) -> UsernameBanDaoData.builder()
        .username(rs.getString(USERNAME_COLUMN))
        .dateCreated(TimestampMapper.map(rs, DATE_CREATED_COLUMN))
        .build();
  }
}
