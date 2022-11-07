package org.triplea.db.dao.access.log;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/** Return data when selecting lobby access history. */
@Getter
public class AccessLogRecord {
  private final Instant accessTime;
  private final String username;
  private final String ip;
  private final String systemId;
  private final boolean registered;

  @Builder
  public AccessLogRecord(
      @ColumnName("access_time") final Instant accessTime,
      @ColumnName("username") final String username,
      @ColumnName("ip") final String ip,
      @ColumnName("system_id") final String systemId,
      @ColumnName("registered") final boolean registered) {
    this.accessTime = accessTime;
    this.username = username;
    this.ip = ip;
    this.systemId = systemId;
    this.registered = registered;
  }
}
