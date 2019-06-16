package org.triplea.lobby.server.db.data;

import java.time.Instant;

import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.lobby.server.db.TimestampMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * Return data when querying for a users API key. Note we return pretty limited information
 * to avoid leaking keys, we return a public id that can be referenced for deleting the key.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ApiKeyDaoData {

  public static final String PUBLIC_ID_COLUMN = "public_id";
  public static final String DATE_LAST_USED_COLUMN = "date_last_used";
  public static final String LAST_USED_HOST_ADDRESS_COLUMN = "last_used_host_address";

  private String publicId;
  private Instant lastUsed;
  private String lastUsedByHostAddress;

  /**
   * Returns a JDBI row mapper used to convert results into an instance of this bean object.
   */
  public static RowMapper<ApiKeyDaoData> buildResultMapper() {
    return (rs, ctx) -> ApiKeyDaoData.builder()
        .publicId(rs.getString(PUBLIC_ID_COLUMN))
        .lastUsed(TimestampMapper.map(rs, DATE_LAST_USED_COLUMN))
        .lastUsedByHostAddress(rs.getString(LAST_USED_HOST_ADDRESS_COLUMN))
        .build();
  }
}
