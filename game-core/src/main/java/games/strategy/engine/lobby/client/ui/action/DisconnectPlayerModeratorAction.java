package games.strategy.engine.lobby.client.ui.action;

import javax.annotation.Nonnull;
import javax.swing.Action;
import javax.swing.JFrame;
import lombok.Builder;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.swing.SwingAction;

@Builder
public class DisconnectPlayerModeratorAction {
  @Nonnull private final JFrame parent;
  @Nonnull private final ModeratorChatClient moderatorLobbyClient;
  @Nonnull private final PlayerName playerName;
  @Nonnull private final PlayerChatId playerChatId;

  public Action toSwingAction() {
    return SwingAction.of(
        "Disconnect " + playerName,
        e -> {
          if (new ActionConfirmation(parent).confirm(("Disconnect " + playerName))) {
            moderatorLobbyClient.disconnectPlayer(playerChatId);
          }
        });
  }
}
