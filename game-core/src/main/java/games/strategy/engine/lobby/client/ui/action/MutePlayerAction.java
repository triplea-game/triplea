package games.strategy.engine.lobby.client.ui.action;

import games.strategy.engine.lobby.client.ui.action.ActionDurationDialog.ActionName;
import java.util.List;
import javax.annotation.Nonnull;
import javax.swing.Action;
import javax.swing.JFrame;
import lombok.Builder;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

@Builder
public class MutePlayerAction {
  @Nonnull private final JFrame parent;
  @Nonnull private final PlayerToLobbyConnection playerToLobbyConnection;
  @Nonnull private final PlayerChatId playerChatId;
  @Nonnull private final String playerName;

  public Action toSwingAction() {
    return SwingAction.of("Mute Player (WIP)", this::runMuteAction);
  }

  private void runMuteAction() {
    ActionDurationDialog.builder()
        .parent(parent)
        .actionName(ActionName.MUTE)
        .maxDuration(60)
        .timeUnits(List.of(ActionTimeUnit.MINUTES))
        .build()
        .prompt()
        .ifPresent(this::confirmMuteAndApply);
  }

  private void confirmMuteAndApply(final ActionDuration actionDuration) {
    SwingComponents.promptUser(
        "Confirm Mute",
        "Are you sure you want to mute " + playerName + " for " + actionDuration,
        () -> {
          playerToLobbyConnection.mutePlayer(playerChatId, actionDuration.toMinutes());
          SwingComponents.showDialog(
              playerName + " muted", playerName + " was muted for " + actionDuration);
        });
  }
}
