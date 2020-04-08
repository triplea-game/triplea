package games.strategy.engine.lobby.client.ui.action;

import javax.annotation.Nonnull;
import javax.swing.Action;
import javax.swing.JFrame;
import lombok.Builder;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.swing.SwingAction;

@Builder
public class BanPlayerModeratorAction {
  @Nonnull private final PlayerToLobbyConnection playerToLobbyConnection;
  @Nonnull private final JFrame parent;
  @Nonnull private final PlayerChatId playerChatIdToBan;

  public Action toSwingAction() {
    return SwingAction.of(
        "Ban Player",
        e ->
            BanDurationDialog.prompt(
                parent,
                timespan ->
                    // do confirmation
                    playerToLobbyConnection.banPlayer(
                        BanPlayerRequest.builder()
                            .playerChatId(playerChatIdToBan.getValue())
                            .banMinutes(timespan.toMinutes())
                            .build())));
  }
}
