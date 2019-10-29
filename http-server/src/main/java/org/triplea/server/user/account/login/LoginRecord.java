package org.triplea.server.user.account.login;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.PlayerName;
import org.triplea.domain.data.SystemId;

@Builder
@Value
public class LoginRecord {
  @Nonnull private final PlayerName playerName;
  @Nonnull private String ip;
  @Nonnull private final SystemId systemId;
  @Nonnull private final PlayerChatId playerChatId;
  private final boolean registered;
}
