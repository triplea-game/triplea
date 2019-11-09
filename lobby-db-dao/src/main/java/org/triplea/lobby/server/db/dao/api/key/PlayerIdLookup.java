package org.triplea.lobby.server.db.dao.api.key;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import org.triplea.domain.data.PlayerName;
import org.triplea.domain.data.SystemId;

@Getter
@Builder
public class PlayerIdLookup {

  @Nonnull private final PlayerName playerName;
  @Nonnull private final SystemId systemId;
  @Nonnull private final String ip;
}
