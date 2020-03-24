package org.triplea.db.dao.user.ban;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.db.TimestampMapper;

/**
 * Lookup record to determine if a player is banned. If so, gives enough information to inform the
 * player of the ban duration and the 'public id' of the ban.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class BanLookupRecord {

  private String publicBanId;
  private Instant banExpiry;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<BanLookupRecord> buildResultMapper() {
    return (rs, ctx) ->
        BanLookupRecord.builder()
            .publicBanId(rs.getString(BanTableColumns.PUBLIC_ID_COLUMN))
            .banExpiry(TimestampMapper.map(rs, BanTableColumns.BAN_EXPIRY_COLUMN))
            .build();
  }
}
