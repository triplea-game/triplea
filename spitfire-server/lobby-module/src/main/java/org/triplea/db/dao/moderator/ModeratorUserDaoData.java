package org.triplea.db.dao.moderator;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/** Return data when querying the user table for moderators. */
@Getter
public class ModeratorUserDaoData {
  private final String username;
  private final Instant lastLogin;

  @Builder
  public ModeratorUserDaoData(
      @ColumnName("username") final String username,
      @ColumnName("access_time") final Instant lastLogin) {
    this.username = username;
    this.lastLogin = lastLogin;
  }
}
