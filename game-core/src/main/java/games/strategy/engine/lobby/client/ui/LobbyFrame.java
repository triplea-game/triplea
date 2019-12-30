package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatMessagePanel;
import games.strategy.engine.chat.ChatMessagePanel.ChatSoundProfile;
import games.strategy.engine.chat.ChatPlayerPanel;
import games.strategy.engine.chat.ChatTransmitter;
import games.strategy.engine.chat.LobbyChatTransmitter;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.ui.action.BanPlayerModeratorAction;
import games.strategy.engine.lobby.client.ui.action.DisconnectPlayerModeratorAction;
import games.strategy.triplea.ui.menubar.LobbyMenu;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import lombok.Getter;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.live.servers.ServerProperties;
import org.triplea.swing.JFrameBuilder;

/** The top-level frame window for the lobby client UI. */
public class LobbyFrame extends JFrame {
  private static final long serialVersionUID = -388371674076362572L;

  @Getter private final LobbyClient lobbyClient;

  public LobbyFrame(final LobbyClient lobbyClient, final ServerProperties serverProperties) {
    super("TripleA Lobby");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setIconImage(JFrameBuilder.getGameIcon());
    this.lobbyClient = lobbyClient;
    setJMenuBar(new LobbyMenu(this));
    final ChatTransmitter chatTransmitter = new LobbyChatTransmitter(lobbyClient);
    final Chat chat = new Chat(chatTransmitter);
    final ChatMessagePanel chatMessagePanel = new ChatMessagePanel(chat, ChatSoundProfile.LOBBY);
    chatMessagePanel.addServerMessage(serverProperties.getMessage());
    final ChatPlayerPanel chatPlayers = new ChatPlayerPanel(chat);
    chatPlayers.setPreferredSize(new Dimension(200, 600));
    chatPlayers.addActionFactory(this::newModeratorActions);

    final LobbyGameTableModel tableModel =
        new LobbyGameTableModel(lobbyClient.isModerator(), lobbyClient.getHttpLobbyClient());
    final LobbyGamePanel gamePanel =
        new LobbyGamePanel(lobbyClient, serverProperties.getUri(), tableModel);

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
        .getHttpLobbyClient()
        .getLobbyChatClient()
        .addConnectionClosedListener(
            () -> {
              tableModel.shutdown();
              dispose();
              shutdown();
            });
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(final WindowEvent e) {
            chatTransmitter.disconnect();
            tableModel.shutdown();
            shutdown();
          }
        });
  }

  private List<Action> newModeratorActions(final ChatParticipant clickedOn) {
    if (!lobbyClient.isModerator()) {
      return List.of();
    }

    if (clickedOn.getUserName().equals(lobbyClient.getUserName())) {
      return List.of();
    }

    final var moderatorLobbyClient = lobbyClient.getHttpLobbyClient().getModeratorLobbyClient();

    return List.of(
        DisconnectPlayerModeratorAction.builder()
            .parent(this)
            .moderatorLobbyClient(moderatorLobbyClient)
            .playerChatId(clickedOn.getPlayerChatId())
            .userName(clickedOn.getUserName())
            .build()
            .toSwingAction(),
        BanPlayerModeratorAction.builder()
            .parent(this)
            .moderatorLobbyClient(moderatorLobbyClient)
            .playerChatIdToBan(clickedOn.getPlayerChatId())
            .build()
            .toSwingAction());
  }

  public void shutdown() {
    setVisible(false);
    dispose();
    new Thread(
            () -> {
              GameRunner.showMainFrame();
              GameRunner.exitGameIfFinished();
            })
        .start();
  }
}
