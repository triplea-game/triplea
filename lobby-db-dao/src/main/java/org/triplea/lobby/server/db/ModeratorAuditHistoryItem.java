package org.triplea.lobby.server.db;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

import org.jdbi.v3.core.mapper.RowMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Bean class for transport between JDBI and http-server. Represents one row with
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ModeratorAuditHistoryItem {
  private Instant dateCreated;
  private String username;
  private String actionName;
  private String actionTarget;

  /**
   * Returns a JDBI row mapper used to convert results into an instance of this bean object.
   */
  static RowMapper<ModeratorAuditHistoryItem> moderatorAuditHistoryItemMapper() {
    return (rs, ctx) -> {
      final Calendar cal = Calendar.getInstance();
      cal.setTimeZone(TimeZone.getTimeZone("UTC"));
      final Timestamp timestamp = rs.getTimestamp(ModeratorAuditHistoryDao.LookupHistoryItemsColumns.DATE_CREATED, cal);

      return ModeratorAuditHistoryItem.builder()
          .dateCreated(timestamp.toInstant())
          .username(rs.getString(ModeratorAuditHistoryDao.LookupHistoryItemsColumns.USER_NAME))
          .actionName(rs.getString(ModeratorAuditHistoryDao.LookupHistoryItemsColumns.ACTION_NAME))
          .actionTarget(rs.getString(ModeratorAuditHistoryDao.LookupHistoryItemsColumns.ACTION_TARGET))
          .build();
    };
  }
}
