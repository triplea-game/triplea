package org.triplea.db.dao.username.ban;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/** Return data when querying the banned username table. */
@Getter
public class UsernameBanRecord {
  private final String username;
  private final Instant dateCreated;

  @Builder
  public UsernameBanRecord(
      @ColumnName("username") final String username,
      @ColumnName("date_created") final Instant dateCreated) {
    this.username = username;
    this.dateCreated = dateCreated;
  }
}
