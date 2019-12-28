package org.triplea.server.user.account.login;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;

@Builder
@Value
public class LoginRecord {
  @Nonnull private final UserName userName;
  @Nonnull private String ip;
  @Nonnull private final SystemId systemId;
  @Nonnull private final PlayerChatId playerChatId;
  private final boolean registered;
}
