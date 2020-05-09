package org.triplea.db.dao.moderator;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.db.TimestampMapper;

/** Return data when selecting moderator audit history. */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ModeratorAuditHistoryRecord {
  private Instant dateCreated;
  private String username;
  private String actionName;
  private String actionTarget;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<ModeratorAuditHistoryRecord> buildResultMapper() {
    return (rs, ctx) ->
        ModeratorAuditHistoryRecord.builder()
            .dateCreated(TimestampMapper.map(rs, "date_created"))
            .username(rs.getString("username"))
            .actionName(rs.getString("action_name"))
            .actionTarget(rs.getString("action_target"))
            .build();
  }
}
