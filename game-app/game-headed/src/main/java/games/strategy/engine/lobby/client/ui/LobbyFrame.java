package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatMessagePanel;
import games.strategy.engine.chat.ChatMessagePanel.ChatSoundProfile;
import games.strategy.engine.chat.ChatPlayerPanel;
import games.strategy.engine.chat.ChatTransmitter;
import games.strategy.engine.chat.LobbyChatTransmitter;
import games.strategy.engine.lobby.client.login.LoginResult;
import games.strategy.engine.lobby.client.ui.action.BanPlayerModeratorAction;
import games.strategy.engine.lobby.client.ui.action.DisconnectPlayerModeratorAction;
import games.strategy.engine.lobby.client.ui.action.MutePlayerAction;
import games.strategy.engine.lobby.client.ui.action.player.info.ShowPlayerInformationAction;
import games.strategy.triplea.EngineImageLoader;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.QuitHandler;
import games.strategy.triplea.ui.menubar.LobbyMenu;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Optional;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import lombok.Getter;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.game.client.HeadedGameRunner;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.sound.ClipPlayer;
import org.triplea.swing.DialogBuilder;
import org.triplea.swing.SwingComponents;

/** The top-level frame window for the lobby client UI. */
public class LobbyFrame extends JFrame implements QuitHandler {
  private static final long serialVersionUID = -388371674076362572L;

  @Getter private final LoginResult loginResult;
  private final ChatTransmitter chatTransmitter;
  private final LobbyGameTableModel tableModel;

  public LobbyFrame(final LoginResult loginResult) {
    super("TripleA Lobby");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    SwingComponents.addWindowClosedListener(this, HeadedGameRunner::exitGameIfNoWindowsVisible);
    setIconImage(EngineImageLoader.loadFrameIcon());
    this.loginResult = loginResult;

    PlayerToLobbyConnection playerToLobbyConnection =
        new PlayerToLobbyConnection(
            ClientSetting.lobbyUri.getValueOrThrow(),
            loginResult.getApiKey(),
            error -> SwingComponents.showError(this, "Error communicating with lobby", error));

    setJMenuBar(new LobbyMenu(this, loginResult, playerToLobbyConnection));
    playerToLobbyConnection.addConnectionTerminatedListener(
        reason -> {
          DialogBuilder.builder()
              .parent(this)
              .title("Connection to Lobby Closed")
              .errorMessage("Connection closed: " + reason)
              .showDialog();
          shutdown();
        });
    playerToLobbyConnection.addConnectionClosedListener(this::shutdown);

    chatTransmitter = new LobbyChatTransmitter(playerToLobbyConnection, loginResult.getUsername());
    final Chat chat = new Chat(chatTransmitter);
    final ChatMessagePanel chatMessagePanel =
        new ChatMessagePanel(chat, ChatSoundProfile.LOBBY, new ClipPlayer());
    Optional.ofNullable(loginResult.getLoginMessage())
        .ifPresent(chatMessagePanel::addServerMessage);
    final ChatPlayerPanel chatPlayers = new ChatPlayerPanel(chat);
    chatPlayers.setPreferredSize(new Dimension(200, 600));
    chatPlayers.addActionFactory(
        clickedChatter ->
            lobbyPlayerRightClickMenuActions(
                this, loginResult, clickedChatter, playerToLobbyConnection));

    tableModel = new LobbyGameTableModel(loginResult.isModerator(), playerToLobbyConnection);
    final LobbyGamePanel gamePanel =
        new LobbyGamePanel(this, loginResult, tableModel, playerToLobbyConnection);

    final JSplitPane leftSplit = new JSplitPane();
    leftSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
    leftSplit.setTopComponent(gamePanel);
    leftSplit.setBottomComponent(chatMessagePanel);
    leftSplit.setResizeWeight(0.5);
    gamePanel.setPreferredSize(new Dimension(700, 200));
    chatMessagePanel.setPreferredSize(new Dimension(700, 400));
    final JSplitPane mainSplit = new JSplitPane();
    mainSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    mainSplit.setLeftComponent(leftSplit);
    mainSplit.setRightComponent(chatPlayers);
    mainSplit.setResizeWeight(1);
    add(mainSplit, BorderLayout.CENTER);
    pack();
    chatMessagePanel.requestFocusInWindow();
    setLocationRelativeTo(null);
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(final WindowEvent e) {
            shutdown();
          }
        });
  }

  private static List<Action> lobbyPlayerRightClickMenuActions(
      JFrame parentWindow,
      LoginResult loginResult,
      ChatParticipant clickedOn,
      PlayerToLobbyConnection playerToLobbyConnection) {
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

  @Override
  public boolean shutdown() {
    setVisible(false);
    dispose();
    chatTransmitter.disconnect();
    tableModel.shutdown();
    return true;
  }
}
