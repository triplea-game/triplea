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
  @Nonnull private final UserName userName;
  @Nonnull private final SystemId systemId;
  @Nonnull private final String ip;

  public static RowMapper<GamePlayerLookup> buildResultMapper() {
    return (rs, ctx) ->
        GamePlayerLookup.builder()
            .userName(UserName.of(rs.getString("username")))
            .systemId(SystemId.of(rs.getString("system_id")))
            .ip(rs.getString("ip"))
            .build();
  }
}
