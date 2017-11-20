package games.strategy.engine.lobby.client.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;

import com.google.common.collect.ImmutableList;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatMessagePanel;
import games.strategy.engine.chat.ChatPlayerPanel;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.server.IModeratorController;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.ModeratorController;
import games.strategy.net.INode;
import games.strategy.triplea.ui.menubar.LobbyMenu;
import games.strategy.ui.SwingAction;
import games.strategy.util.EventThreadJOptionPane;

public class LobbyFrame extends JFrame {
  private static final long serialVersionUID = -388371674076362572L;

  private static final List<String> banOrMuteOptions = ImmutableList.of(
      "Mac Address Only",
      "User Name only",
      "Name and Mac",
      "Cancel");

  private final LobbyClient client;
  private final ChatMessagePanel chatMessagePanel;

  public LobbyFrame(final LobbyClient client, final LobbyServerProperties lobbyServerProperties) {
    super("TripleA Lobby");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setIconImage(GameRunner.getGameIcon(this));
    this.client = client;
    setJMenuBar(new LobbyMenu(this));
    final Chat chat = new Chat(this.client.getMessenger(), LobbyServer.LOBBY_CHAT,
        this.client.getChannelMessenger(), this.client.getRemoteMessenger(), Chat.ChatSoundProfile.LOBBY_CHATROOM);
    chatMessagePanel = new ChatMessagePanel(chat);
    showServerMessage(lobbyServerProperties);
    chatMessagePanel.setShowTime(true);
    final ChatPlayerPanel chatPlayers = new ChatPlayerPanel(null);
    chatPlayers.addHiddenPlayerName(LobbyServer.ADMIN_USERNAME);
    chatPlayers.setChat(chat);
    chatPlayers.setPreferredSize(new Dimension(200, 600));
    chatPlayers.addActionFactory(clickedOn -> createAdminActions(clickedOn));
    final LobbyGamePanel gamePanel = new LobbyGamePanel(this.client.getMessengers());
    final JSplitPane leftSplit = new JSplitPane();
    leftSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
    leftSplit.setTopComponent(gamePanel);
    leftSplit.setBottomComponent(chatMessagePanel);
    leftSplit.setResizeWeight(0.8);
    gamePanel.setPreferredSize(new Dimension(700, 200));
    chatMessagePanel.setPreferredSize(new Dimension(700, 400));
    final JSplitPane mainSplit = new JSplitPane();
    mainSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    mainSplit.setLeftComponent(leftSplit);
    mainSplit.setRightComponent(chatPlayers);
    add(mainSplit, BorderLayout.CENTER);
    pack();
    chatMessagePanel.requestFocusInWindow();
    setLocationRelativeTo(null);
    this.client.getMessenger().addErrorListener((messenger, reason) -> connectionToServerLost());
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        shutdown();
      }
    });
  }

  public ChatMessagePanel getChatMessagePanel() {
    return chatMessagePanel;
  }

  private void showServerMessage(final LobbyServerProperties props) {
    if (!props.serverMessage.isEmpty()) {
      chatMessagePanel.addServerMessage(props.serverMessage);
    }
  }

  private List<Action> createAdminActions(final INode clickedOn) {
    if (!client.isAdmin()) {
      return Collections.emptyList();
    }
    if (clickedOn.equals(client.getMessenger().getLocalNode())) {
      return Collections.emptyList();
    }
    final IModeratorController controller = (IModeratorController) client.getRemoteMessenger()
        .getRemote(ModeratorController.getModeratorControllerName());
    final List<Action> actions = new ArrayList<>();
    actions.add(SwingAction.of("Boot " + clickedOn.getName(), e -> {
      if (!confirm("Boot " + clickedOn.getName())) {
        return;
      }
      controller.boot(clickedOn);
    }));
    actions.add(SwingAction.of("Ban Player", e -> {
      final int resultBanType = JOptionPane.showOptionDialog(LobbyFrame.this,
          "Select the type of ban:",
          "Select Ban Type", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
          banOrMuteOptions.toArray(), banOrMuteOptions.get(banOrMuteOptions.size() - 1));
      if (resultBanType < 0) {
        return;
      }
      final String selectedBanType = banOrMuteOptions.get(resultBanType);
      if (selectedBanType.equals("Cancel")) {
        return;
      }

      TimespanDialog.prompt(this, "Select Timespan",
          "Please consult other admins before banning longer than 1 day. \n"
              + "And please remember to report this ban.",
          date -> {
            if (selectedBanType.toLowerCase().contains("name")) {
              controller.banUsername(clickedOn, date);
            }
            if (selectedBanType.toLowerCase().contains("mac")) {
              controller.banMac(clickedOn, date);
            }
            // Should we keep this auto?
            controller.boot(clickedOn);
          });
    }));

    actions.add(SwingAction.of("Mute Player", e -> {
      final int resultMuteType = JOptionPane.showOptionDialog(LobbyFrame.this,
          "<html>Select the type of mute: <br>Please consult other admins before muting longer than 1 day.</html>",
          "Select Mute Type", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
          banOrMuteOptions.toArray(), banOrMuteOptions.get(banOrMuteOptions.size() - 1));
      if (resultMuteType < 0) {
        return;
      }
      final String selectedMuteType = banOrMuteOptions.get(resultMuteType);
      if (selectedMuteType.equals("Cancel")) {
        return;
      }
      TimespanDialog.prompt(this, "Select Timespan",
          "Please consult other admins before muting longer than 1 day. \n"
              + "And please remember to report this mute.",
          date -> {
            if (selectedMuteType.toLowerCase().contains("name")) {
              controller.muteUsername(clickedOn, date);
            }
            if (selectedMuteType.toLowerCase().contains("mac")) {
              controller.muteMac(clickedOn, date);
            }
          });
    }));
    actions.add(SwingAction.of("Quick Mute", e -> {
      final JLabel label = new JLabel("How many minutes should this player be muted?");
      final JSpinner spinner = new JSpinner(new SpinnerNumberModel(10, 0, 60 * 24 * 2, 1));
      final JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
      panel.add(label);
      panel.add(spinner);
      if (JOptionPane.showConfirmDialog(LobbyFrame.this, panel, "Mute Player",
          JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        final Object value = spinner.getValue();
        if (value == null) {
          return;
        }
        final long resultMuteLengthInMinutes = Long.parseLong(value.toString());
        if (resultMuteLengthInMinutes < 0) {
          return;
        }
        final Instant expire = Instant.now().plus(Duration.ofMinutes(resultMuteLengthInMinutes));
        controller.muteUsername(clickedOn, Date.from(expire));
        controller.muteMac(clickedOn, Date.from(expire));
      }
    }));
    actions.add(SwingAction.of("Show player information", e -> {
      final String text = controller.getInformationOn(clickedOn);
      final JTextPane textPane = new JTextPane();
      textPane.setEditable(false);
      textPane.setText(text);
      JOptionPane.showMessageDialog(null, textPane, "Player Info", JOptionPane.INFORMATION_MESSAGE);
    }));
    return actions;
  }

  private boolean confirm(final String question) {
    final int selectionOption = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(this), question,
        "Question", JOptionPane.OK_CANCEL_OPTION);
    return selectionOption == JOptionPane.OK_OPTION;
  }

  public LobbyClient getLobbyClient() {
    return client;
  }

  public void setShowChatTime(final boolean showTime) {
    if (chatMessagePanel != null) {
      chatMessagePanel.setShowTime(showTime);
    }
  }

  public void shutdown() {
    setVisible(false);
    dispose();
    GameRunner.showMainFrame();
    client.getMessenger().shutDown();
    GameRunner.exitGameIfFinished();
  }

  private void connectionToServerLost() {
    EventThreadJOptionPane.showMessageDialog(LobbyFrame.this,
        "Connection to Server Lost.  Please close this instance and reconnect to the lobby.", "Connection Lost",
        JOptionPane.ERROR_MESSAGE);
  }
}
