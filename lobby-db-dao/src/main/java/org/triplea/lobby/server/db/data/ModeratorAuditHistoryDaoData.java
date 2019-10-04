package org.triplea.lobby.server.db.data;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.lobby.server.db.TimestampMapper;

/** Return data when selecting moderator audit history. */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ModeratorAuditHistoryDaoData {
  public static final String USER_NAME_COLUMN = "username";
  public static final String DATE_CREATED_COLUMN = "date_created";
  public static final String ACTION_NAME_COLUMN = "action_name";
  public static final String ACTION_TARGET_COLUMN = "action_target";

  private Instant dateCreated;
  private String username;
  private String actionName;
  private String actionTarget;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<ModeratorAuditHistoryDaoData> buildResultMapper() {
    return (rs, ctx) ->
        ModeratorAuditHistoryDaoData.builder()
            .dateCreated(TimestampMapper.map(rs, DATE_CREATED_COLUMN))
            .username(rs.getString(USER_NAME_COLUMN))
            .actionName(rs.getString(ACTION_NAME_COLUMN))
            .actionTarget(rs.getString(ACTION_TARGET_COLUMN))
            .build();
  }
}
