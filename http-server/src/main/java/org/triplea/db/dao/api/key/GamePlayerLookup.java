package org.triplea.db.dao.api.key;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;

@Getter
@Builder
@EqualsAndHashCode
public class GamePlayerLookup {
  static final String PLAYER_NAME_COLUMN = "username";
  static final String SYSTEM_ID_COLUMN = "system_id";
  static final String IP_COLUMN = "ip";

  @Nonnull private final UserName userName;
  @Nonnull private final SystemId systemId;
  @Nonnull private final String ip;

  public static RowMapper<GamePlayerLookup> buildResultMapper() {
    return (rs, ctx) ->
        GamePlayerLookup.builder()
            .userName(UserName.of(rs.getString(PLAYER_NAME_COLUMN)))
            .systemId(SystemId.of(rs.getString(SYSTEM_ID_COLUMN)))
            .ip(rs.getString(IP_COLUMN))
            .build();
  }
}
