package games.strategy.engine.lobby.client;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.HttpLobbyClient;

/** Provides information about a client connection to a lobby server. */
@Getter
@Builder
public class LobbyClient {
  @Nonnull private final HttpLobbyClient httpLobbyClient;
  @Nonnull private final UserName userName;
  private final boolean anonymousLogin;
  private final boolean moderator;
  private final boolean passwordChangeRequired;
}
