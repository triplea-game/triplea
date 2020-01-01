package games.strategy.engine.lobby.client.ui.action;

import javax.annotation.Nonnull;
import javax.swing.Action;
import javax.swing.JFrame;
import lombok.Builder;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.swing.SwingAction;

@Builder
public class DisconnectPlayerModeratorAction {
  @Nonnull private final JFrame parent;
  @Nonnull private final ModeratorChatClient moderatorLobbyClient;
  @Nonnull private final UserName userName;
  @Nonnull private final PlayerChatId playerChatId;

  public Action toSwingAction() {
    return SwingAction.of(
        "Disconnect " + userName,
        e -> {
          if (new ActionConfirmation(parent).confirm(("Disconnect " + userName))) {
            moderatorLobbyClient.disconnectPlayer(playerChatId);
          }
        });
  }
}
