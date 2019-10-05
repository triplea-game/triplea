package org.triplea.lobby.server.db.data;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.lobby.server.db.TimestampMapper;

/**
 * Return data when querying about user bans. The public ban id is for public reference, this is a
 * value we can show to banned users so they can report which ban is impacting them. With that
 * information we have the ability to remove the ban without needing to ask them for mac or IP.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserBanDaoData {
  public static final String PUBLIC_ID_COLUMN = "public_id";
  public static final String USERNAME_COLUMN = "username";
  public static final String HASHED_MAC_COLUMN = "hashed_mac";
  public static final String IP_COLUMN = "ip";
  public static final String BAN_EXPIRY_COLUMN = "ban_expiry";
  public static final String DATE_CREATED_COLUMN = "date_created";

  private String publicBanId;
  private String username;
  private String hashedMac;
  private String ip;
  private Instant banExpiry;
  private Instant dateCreated;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<UserBanDaoData> buildResultMapper() {
    return (rs, ctx) ->
        UserBanDaoData.builder()
            .publicBanId(rs.getString(PUBLIC_ID_COLUMN))
            .username(rs.getString(USERNAME_COLUMN))
            .hashedMac(rs.getString(HASHED_MAC_COLUMN))
            .ip(rs.getString(IP_COLUMN))
            .dateCreated(TimestampMapper.map(rs, DATE_CREATED_COLUMN))
            .banExpiry(TimestampMapper.map(rs, BAN_EXPIRY_COLUMN))
            .build();
  }
}
