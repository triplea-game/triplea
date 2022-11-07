package org.triplea.db.dao.user.ban;

import java.time.Instant;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * Lookup record to determine if a player is banned. If so, gives enough information to inform the
 * player of the ban duration and the 'public id' of the ban.
 */
@Getter
@EqualsAndHashCode
public class BanLookupRecord {

  private final String publicBanId;
  private final Instant banExpiry;

  @Builder
  public BanLookupRecord(
      @ColumnName("public_id") final String publicBanId,
      @ColumnName("ban_expiry") final Instant banExpiry) {
    this.publicBanId = publicBanId;
    this.banExpiry = banExpiry;
  }
}
