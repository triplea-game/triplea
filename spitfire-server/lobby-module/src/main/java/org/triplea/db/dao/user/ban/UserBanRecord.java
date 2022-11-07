package org.triplea.db.dao.user.ban;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * Return data when querying about user bans. The public ban id is for public reference, this is a
 * value we can show to banned users so they can report which ban is impacting them. With that
 * information we have the ability to remove the ban without needing to ask them for mac or IP.
 */
@Getter
public class UserBanRecord {

  private final String publicBanId;
  private final String username;
  private final String systemId;
  private final String ip;
  private final Instant banExpiry;
  private final Instant dateCreated;

  @Builder
  public UserBanRecord(
      @ColumnName("public_id") final String publicBanId,
      @ColumnName("username") final String username,
      @ColumnName("system_id") final String systemId,
      @ColumnName("ip") final String ip,
      @ColumnName("ban_expiry") final Instant banExpiry,
      @ColumnName("date_created") final Instant dateCreated) {
    this.publicBanId = publicBanId;
    this.username = username;
    this.systemId = systemId;
    this.ip = ip;
    this.banExpiry = banExpiry;
    this.dateCreated = dateCreated;
  }
}
