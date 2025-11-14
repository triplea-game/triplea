package games.strategy.engine.lobby.client.ui.action;

import games.strategy.engine.lobby.client.ui.action.ActionDurationDialog.ActionName;
import javax.annotation.Nonnull;
import javax.swing.Action;
import javax.swing.JFrame;
import lombok.Builder;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

/** Shows a pop-up for moderators confirming a player ban and for how long. */
@Builder
public class BanPlayerModeratorAction {
  @Nonnull private final PlayerToLobbyConnection playerToLobbyConnection;
  @Nonnull private final JFrame parent;
  @Nonnull private final PlayerChatId playerChatIdToBan;
  @Nonnull private final String playerName;

  public Action toSwingAction() {
    return SwingAction.of(
        "Ban Player",
        e ->
            ActionDurationDialog.builder()
                .parent(parent)
                .actionName(ActionName.BAN)
                .build()
                .prompt()
                .ifPresent(this::banAction));
  }

  private void banAction(final ActionDuration timespan) {
    SwingComponents.promptUser(
        "Confirm Ban",
        "Are you sure you want to ban " + playerName + " for " + timespan,
        () -> {
          playerToLobbyConnection.banPlayer(
              BanPlayerRequest.builder()
                  .playerChatId(playerChatIdToBan.getValue())
                  .banMinutes(timespan.toMinutes())
                  .build());

          SwingComponents.showDialog(
              parent, playerName + " banned", playerName + " was banned for " + timespan);
        });
  }
}
