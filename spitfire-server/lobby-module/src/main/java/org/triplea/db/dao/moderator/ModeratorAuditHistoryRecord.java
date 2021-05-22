package org.triplea.db.dao.moderator;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/** Return data when selecting moderator audit history. */
@Getter
public class ModeratorAuditHistoryRecord {
  private final Instant dateCreated;
  private final String username;
  private final String actionName;
  private final String actionTarget;

  @Builder
  public ModeratorAuditHistoryRecord(
      @ColumnName("date_created") final Instant dateCreated,
      @ColumnName("username") final String username,
      @ColumnName("action_name") final String actionName,
      @ColumnName("action_target") final String actionTarget) {
    this.dateCreated = dateCreated;
    this.username = username;
    this.actionName = actionName;
    this.actionTarget = actionTarget;
  }
}
