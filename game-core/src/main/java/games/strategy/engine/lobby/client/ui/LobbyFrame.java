package games.strategy.engine.lobby.client.ui;

import com.google.common.collect.ImmutableList;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatMessagePanel;
import games.strategy.engine.chat.ChatParticipant;
import games.strategy.engine.chat.ChatPlayerPanel;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.triplea.ui.menubar.LobbyMenu;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import org.triplea.lobby.common.IModeratorController;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.swing.DialogBuilder;
import org.triplea.swing.EventThreadJOptionPane;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.SwingAction;

/** The top-level frame window for the lobby client UI. */
public class LobbyFrame extends JFrame {
  private static final long serialVersionUID = -388371674076362572L;

  private final LobbyClient client;

  public LobbyFrame(final LobbyClient client, final LobbyServerProperties lobbyServerProperties) {
    super("TripleA Lobby");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setIconImage(JFrameBuilder.getGameIcon());
    this.client = client;
    setJMenuBar(new LobbyMenu(this));
    final Chat chat =
        new Chat(
            client.getMessengers(),
            LobbyConstants.LOBBY_CHAT,
            Chat.ChatSoundProfile.LOBBY_CHATROOM);
    final ChatMessagePanel chatMessagePanel = new ChatMessagePanel(chat);
    lobbyServerProperties.getServerMessage().ifPresent(chatMessagePanel::addServerMessage);
    final ChatPlayerPanel chatPlayers = new ChatPlayerPanel(null);
    chatPlayers.addHiddenPlayerName(LobbyConstants.ADMIN_USERNAME);
    chatPlayers.setChat(chat);
    chatPlayers.setPreferredSize(new Dimension(200, 600));
    chatPlayers.addActionFactory(this::newAdminActions);

    final LobbyGameTableModel tableModel =
        new LobbyGameTableModel(
            client.isAdmin(),
            client.getHttpLobbyClient().getGameListingClient(),
            this::reportErrorMessage);
    final LobbyGamePanel gamePanel = new LobbyGamePanel(client, lobbyServerProperties, tableModel);

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
    this.client.getMessengers().addErrorListener((reason) -> connectionToServerLost());
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(final WindowEvent e) {
            tableModel.shutdown();
            shutdown();
          }
        });
  }

  private void reportErrorMessage(final String errorMessage) {
    DialogBuilder.builder()
        .parent(this)
        .title("Lobby not available")
        .errorMessage(
            "Failed to connect to lobby, game listing will not be updated.\n"
                + "Error: "
                + errorMessage)
        .showDialog();
  }

  private List<Action> newAdminActions(final ChatParticipant clickedOn) {
    if (!client.isAdmin()) {
      return Collections.emptyList();
    }
    if (clickedOn.getPlayerName().equals(client.getMessengers().getLocalNode().getPlayerName())) {
      return Collections.emptyList();
    }
    final IModeratorController controller =
        (IModeratorController) client.getMessengers().getRemote(IModeratorController.REMOTE_NAME);
    final List<Action> actions = new ArrayList<>();
    actions.add(
        SwingAction.of(
            "Boot " + clickedOn.getPlayerName(),
            e -> {
              if (!confirm("Boot " + clickedOn.getPlayerName())) {
                return;
              }
              controller.boot(clickedOn.getPlayerName());
            }));
    actions.add(
        SwingAction.of(
            "Ban Player",
            e ->
                TimespanDialog.prompt(
                    this,
                    "Select Timespan",
                    "Please consult other admins before banning longer than 1 day. \n"
                        + "And please remember to report this ban.",
                    date -> {
                      controller.banUser(clickedOn.getPlayerName(), date.toInstant());
                      controller.boot(clickedOn.getPlayerName());
                    })));
    return ImmutableList.copyOf(actions);
  }

  private boolean confirm(final String question) {
    final int selectionOption =
        JOptionPane.showConfirmDialog(
            JOptionPane.getFrameForComponent(this),
            question,
            "Question",
            JOptionPane.OK_CANCEL_OPTION);
    return selectionOption == JOptionPane.OK_OPTION;
  }

  public LobbyClient getLobbyClient() {
    return client;
  }

  public void shutdown() {
    setVisible(false);
    dispose();
    new Thread(
            () -> {
              GameRunner.showMainFrame();
              client.getMessengers().shutDown();
              GameRunner.exitGameIfFinished();
            })
        .start();
  }

  private void connectionToServerLost() {
    EventThreadJOptionPane.showMessageDialog(
        LobbyFrame.this,
        "Connection to Server Lost.  Please close this instance and reconnect to the lobby.",
        "Connection Lost",
        JOptionPane.ERROR_MESSAGE);
  }
}
