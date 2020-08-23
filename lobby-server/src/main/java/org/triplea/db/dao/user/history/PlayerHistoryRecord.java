package org.triplea.db.dao.user.history;

import java.time.Instant;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

@Getter
public class PlayerHistoryRecord {

  private final long registrationDate;

  public PlayerHistoryRecord(@ColumnName("date_registered") final Instant registrationDate) {
    this.registrationDate = registrationDate.toEpochMilli();
  }
}
