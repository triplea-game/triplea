package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.lobby.client.login.LoginResult;
import games.strategy.engine.lobby.client.ui.action.BanPlayerModeratorAction;
import games.strategy.engine.lobby.client.ui.action.DisconnectPlayerModeratorAction;
import games.strategy.engine.lobby.client.ui.action.MutePlayerAction;
import games.strategy.engine.lobby.client.ui.action.player.info.ShowPlayerInformationAction;
import java.util.List;
import javax.swing.Action;
import javax.swing.JFrame;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;

/** Builds the right-click action list for a player entry in the lobby chat panel. */
class LobbyPlayerActions {

  private LobbyPlayerActions() {}

  static List<Action> buildFor(
      final JFrame parentWindow,
      final LoginResult loginResult,
      final ChatParticipant clickedOn,
      final PlayerToLobbyConnection playerToLobbyConnection) {
    if (clickedOn.getUserName().equals(loginResult.getUsername())) {
      return List.of();
    }

    final var showPlayerInformationAction =
        ShowPlayerInformationAction.builder()
            .parent(parentWindow)
            .playerChatId(clickedOn.getPlayerChatId())
            .playerName(clickedOn.getUserName())
            .playerToLobbyConnection(playerToLobbyConnection)
            .build()
            .toSwingAction();

    if (!loginResult.isModerator()) {
      return List.of(showPlayerInformationAction);
    }
    return List.of(
        showPlayerInformationAction,
        MutePlayerAction.builder()
            .parent(parentWindow)
            .playerChatId(clickedOn.getPlayerChatId())
            .playerToLobbyConnection(playerToLobbyConnection)
            .playerName(clickedOn.getUserName().getValue())
            .build()
            .toSwingAction(),
        DisconnectPlayerModeratorAction.builder()
            .parent(parentWindow)
            .playerToLobbyConnection(playerToLobbyConnection)
            .playerChatId(clickedOn.getPlayerChatId())
            .userName(clickedOn.getUserName())
            .build()
            .toSwingAction(),
        BanPlayerModeratorAction.builder()
            .parent(parentWindow)
            .playerToLobbyConnection(playerToLobbyConnection)
            .playerChatIdToBan(clickedOn.getPlayerChatId())
            .playerName(clickedOn.getUserName().getValue())
            .build()
            .toSwingAction());
  }
}
