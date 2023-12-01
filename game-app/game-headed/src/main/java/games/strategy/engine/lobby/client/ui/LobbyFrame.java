package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatMessagePanel;
import games.strategy.engine.chat.ChatMessagePanel.ChatSoundProfile;
import games.strategy.engine.chat.ChatPlayerPanel;
import games.strategy.engine.chat.ChatTransmitter;
import games.strategy.engine.chat.LobbyChatTransmitter;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.ui.action.BanPlayerModeratorAction;
import games.strategy.engine.lobby.client.ui.action.DisconnectPlayerModeratorAction;
import games.strategy.engine.lobby.client.ui.action.MutePlayerAction;
import games.strategy.engine.lobby.client.ui.action.player.info.ShowPlayerInformationAction;
import games.strategy.triplea.EngineImageLoader;
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
import org.triplea.sound.ClipPlayer;
import org.triplea.swing.DialogBuilder;
import org.triplea.swing.SwingComponents;

/** The top-level frame window for the lobby client UI. */
public class LobbyFrame extends JFrame implements QuitHandler {
  private static final long serialVersionUID = -388371674076362572L;

  @Getter private final LobbyClient lobbyClient;
  private final ChatTransmitter chatTransmitter;
  private final LobbyGameTableModel tableModel;

  public LobbyFrame(final LobbyClient lobbyClient) {
    super("TripleA Lobby");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    SwingComponents.addWindowClosedListener(this, HeadedGameRunner::exitGameIfNoWindowsVisible);
    setIconImage(EngineImageLoader.loadFrameIcon());
    this.lobbyClient = lobbyClient;
    setJMenuBar(new LobbyMenu(this));
    chatTransmitter =
        new LobbyChatTransmitter(
            lobbyClient.getPlayerToLobbyConnection(), lobbyClient.getUserName());
    final Chat chat = new Chat(chatTransmitter);
    final ChatMessagePanel chatMessagePanel =
        new ChatMessagePanel(chat, ChatSoundProfile.LOBBY, new ClipPlayer());
    Optional.ofNullable(lobbyClient.getLobbyMessage())
        .ifPresent(chatMessagePanel::addServerMessage);
    final ChatPlayerPanel chatPlayers = new ChatPlayerPanel(chat);
    chatPlayers.setPreferredSize(new Dimension(200, 600));
    chatPlayers.addActionFactory(this::lobbyPlayerRightClickMenuActions);

    tableModel =
        new LobbyGameTableModel(
            lobbyClient.isModerator(), lobbyClient.getPlayerToLobbyConnection());
    final LobbyGamePanel gamePanel = new LobbyGamePanel(this, lobbyClient, tableModel);

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
    lobbyClient
        .getPlayerToLobbyConnection()
        .addConnectionTerminatedListener(
            reason -> {
              DialogBuilder.builder()
                  .parent(this)
                  .title("Connection to Lobby Closed")
                  .errorMessage("Connection closed: " + reason)
                  .showDialog();
              shutdown();
            });
    lobbyClient.getPlayerToLobbyConnection().addConnectionClosedListener(this::shutdown);
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(final WindowEvent e) {
            shutdown();
          }
        });
  }

  private List<Action> lobbyPlayerRightClickMenuActions(final ChatParticipant clickedOn) {
    if (clickedOn.getUserName().equals(lobbyClient.getUserName())) {
      return List.of();
    }

    final var playerToLobbyConnection = lobbyClient.getPlayerToLobbyConnection();

    final var showPlayerInformationAction =
        ShowPlayerInformationAction.builder()
            .parent(this)
            .playerChatId(clickedOn.getPlayerChatId())
            .playerName(clickedOn.getUserName())
            .playerToLobbyConnection(playerToLobbyConnection)
            .build()
            .toSwingAction();

    if (!lobbyClient.isModerator()) {
      return List.of(showPlayerInformationAction);
    }
    return List.of(
        showPlayerInformationAction,
        MutePlayerAction.builder()
            .parent(this)
            .playerChatId(clickedOn.getPlayerChatId())
            .playerToLobbyConnection(playerToLobbyConnection)
            .playerName(clickedOn.getUserName().getValue())
            .build()
            .toSwingAction(),
        DisconnectPlayerModeratorAction.builder()
            .parent(this)
            .playerToLobbyConnection(playerToLobbyConnection)
            .playerChatId(clickedOn.getPlayerChatId())
            .userName(clickedOn.getUserName())
            .build()
            .toSwingAction(),
        BanPlayerModeratorAction.builder()
            .parent(this)
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
