package games.strategy.engine.lobby.client.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.server.IModeratorController;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.ModeratorController;
import games.strategy.net.INode;
import games.strategy.triplea.ui.menubar.LobbyMenu;
import games.strategy.ui.SwingAction;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;

public class LobbyFrame extends JFrame {
  private static final long serialVersionUID = -388371674076362572L;

  private static final List<String> banOrMuteOptions = ImmutableList.of(
      "Mac Address Only",
      "User Name only",
      "Name and Mac",
      "Cancel");

  private final LobbyClient m_client;
  private final ChatMessagePanel m_chatMessagePanel;

  public LobbyFrame(final LobbyClient client, final LobbyServerProperties props) {
    super("TripleA Lobby");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setIconImage(GameRunner.getGameIcon(this));
    m_client = client;
    setJMenuBar(new LobbyMenu(this));
    final Chat chat = new Chat(m_client.getMessenger(), LobbyServer.LOBBY_CHAT, m_client.getChannelMessenger(),
        m_client.getRemoteMessenger(), Chat.CHAT_SOUND_PROFILE.LOBBY_CHATROOM);
    m_chatMessagePanel = new ChatMessagePanel(chat);
    showServerMessage(props);
    m_chatMessagePanel.setShowTime(true);
    final ChatPlayerPanel chatPlayers = new ChatPlayerPanel(null);
    chatPlayers.addHiddenPlayerName(LobbyServer.ADMIN_USERNAME);
    chatPlayers.setChat(chat);
    chatPlayers.setPreferredSize(new Dimension(200, 600));
    chatPlayers.addActionFactory(clickedOn -> createAdminActions(clickedOn));
    final LobbyGamePanel gamePanel = new LobbyGamePanel(m_client.getMessengers());
    final JSplitPane leftSplit = new JSplitPane();
    leftSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
    leftSplit.setTopComponent(gamePanel);
    leftSplit.setBottomComponent(m_chatMessagePanel);
    leftSplit.setResizeWeight(0.8);
    gamePanel.setPreferredSize(new Dimension(700, 200));
    m_chatMessagePanel.setPreferredSize(new Dimension(700, 400));
    final JSplitPane mainSplit = new JSplitPane();
    mainSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    mainSplit.setLeftComponent(leftSplit);
    mainSplit.setRightComponent(chatPlayers);
    add(mainSplit, BorderLayout.CENTER);
    pack();
    m_chatMessagePanel.requestFocusInWindow();
    setLocationRelativeTo(null);
    m_client.getMessenger().addErrorListener((messenger, reason) -> connectionToServerLost());
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        shutdown();
      }
    });
  }

  public ChatMessagePanel getChatMessagePanel() {
    return m_chatMessagePanel;
  }

  private void showServerMessage(final LobbyServerProperties props) {
    if (props.serverMessage != null && props.serverMessage.length() > 0) {
      m_chatMessagePanel.addServerMessage(props.serverMessage);
    }
  }

  private List<Action> createAdminActions(final INode clickedOn) {
    if (!m_client.isAdmin()) {
      return Collections.emptyList();
    }
    if (clickedOn.equals(m_client.getMessenger().getLocalNode())) {
      return Collections.emptyList();
    }
    final IModeratorController controller = (IModeratorController) m_client.getRemoteMessenger()
        .getRemote(ModeratorController.getModeratorControllerName());
    final List<Action> rVal = new ArrayList<>();
    rVal.add(SwingAction.of("Boot " + clickedOn.getName(), e -> {
      if (!confirm("Boot " + clickedOn.getName())) {
        return;
      }
      controller.boot(clickedOn);
    }));
    rVal.add(SwingAction.of("Ban Player", e -> {
      final int resultBanType = JOptionPane.showOptionDialog(LobbyFrame.this,
          "<html>Select the type of ban: <br>"
              + "Please consult other admins before banning longer than 1 day. <br>"
              + "And please remember to report this ban.</html>",
          "Select Ban Type", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
          banOrMuteOptions.toArray(), banOrMuteOptions.toArray()[banOrMuteOptions.size() - 1]);
      if (resultBanType < 0) {
        return;
      }
      final String selectedBanType = (String) banOrMuteOptions.toArray()[resultBanType];
      if (selectedBanType.equals("Cancel")) {
        return;
      }
      final List<String> timeUnits = new ArrayList<>();
      timeUnits.add("Minute");
      timeUnits.add("Hour");
      timeUnits.add("Day");
      timeUnits.add("Week");
      timeUnits.add("Month");
      timeUnits.add("Year");
      timeUnits.add("Forever");
      timeUnits.add("Cancel");
      final int resultTimespanUnit = JOptionPane.showOptionDialog(LobbyFrame.this, "Select the unit of measurement: ",
          "Select Timespan Unit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
          timeUnits.toArray(), timeUnits.toArray()[timeUnits.size() - 1]);
      if (resultTimespanUnit < 0) {
        return;
      }
      final String selectedTimeUnit = (String) timeUnits.toArray()[resultTimespanUnit];
      if (selectedTimeUnit.equalsIgnoreCase("Cancel")) {
        return;
      }
      if (selectedTimeUnit.equals("Forever")) {
        if (selectedBanType.toLowerCase().contains("name")) {
          controller.banUsername(clickedOn, null);
        }
        if (selectedBanType.toLowerCase().contains("mac")) {
          controller.banMac(clickedOn, null);
        }
        // Should we keep this auto?
        controller.boot(clickedOn);
        return;
      }
      final String resultLengthOfTime = JOptionPane.showInputDialog(LobbyFrame.this,
          "Now please enter the length of time to ban the player: (In " + selectedTimeUnit + "s) ", 1);
      if (resultLengthOfTime == null) {
        return;
      }
      final long result2 = Long.parseLong(resultLengthOfTime);
      if (result2 < 0) {
        return;
      }
      long ticks = 0;
      if (selectedTimeUnit.equals("Minute")) {
        ticks = result2 * 1000 * 60;
      } else if (selectedTimeUnit.equals("Hour")) {
        ticks = result2 * 1000 * 60 * 60;
      } else if (selectedTimeUnit.equals("Day")) {
        ticks = result2 * 1000 * 60 * 60 * 24;
      } else if (selectedTimeUnit.equals("Week")) {
        ticks = result2 * 1000 * 60 * 60 * 24 * 7;
      } else if (selectedTimeUnit.equals("Month")) {
        ticks = result2 * 1000 * 60 * 60 * 24 * 30;
      } else if (selectedTimeUnit.equals("Year")) {
        ticks = result2 * 1000 * 60 * 60 * 24 * 365;
      }
      final long expire = System.currentTimeMillis() + ticks;
      if (selectedBanType.toLowerCase().contains("name")) {
        controller.banUsername(clickedOn, new Date(expire));
      }
      if (selectedBanType.toLowerCase().contains("mac")) {
        controller.banMac(clickedOn, new Date(expire));
      }
      // Should we keep this auto?
      controller.boot(clickedOn);
    }));


    rVal.add(SwingAction.of("Mute Player", e -> {
      final int resultMuteType = JOptionPane.showOptionDialog(LobbyFrame.this,
          "<html>Select the type of mute: <br>Please consult other admins before muting longer than 1 day.</html>",
          "Select Mute Type", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
          banOrMuteOptions.toArray(), banOrMuteOptions.toArray()[banOrMuteOptions.size() - 1]);
      if (resultMuteType < 0) {
        return;
      }
      final String selectedMuteType = (String) banOrMuteOptions.toArray()[resultMuteType];
      if (selectedMuteType.equals("Cancel")) {
        return;
      }
      final List<String> timeUnits = new ArrayList<>();
      timeUnits.add("Minute");
      timeUnits.add("Hour");
      timeUnits.add("Day");
      timeUnits.add("Week");
      timeUnits.add("Month");
      timeUnits.add("Year");
      timeUnits.add("Forever");
      timeUnits.add("Cancel");
      final int resultTimespanUnit = JOptionPane.showOptionDialog(LobbyFrame.this, "Select the unit of measurement: ",
          "Select Timespan Unit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
          timeUnits.toArray(), timeUnits.toArray()[timeUnits.size() - 1]);
      if (resultTimespanUnit < 0) {
        return;
      }
      final String selectedTimeUnit = (String) timeUnits.toArray()[resultTimespanUnit];
      if (selectedTimeUnit.equals("Cancel")) {
        return;
      }
      if (selectedTimeUnit.equals("Forever")) {
        if (selectedMuteType.toLowerCase().contains("name")) {
          controller.muteUsername(clickedOn, null);
        }
        if (selectedMuteType.toLowerCase().contains("mac")) {
          controller.muteMac(clickedOn, null);
        }
        return;
      }
      final String resultLengthOfTime = JOptionPane.showInputDialog(LobbyFrame.this,
          "Now please enter the length of time to mute the player: (In " + selectedTimeUnit + "s) ", 1);
      if (resultLengthOfTime == null) {
        return;
      }
      final long result2 = Long.parseLong(resultLengthOfTime);
      if (result2 < 0) {
        return;
      }
      long ticks = 0;
      if (selectedTimeUnit.equals("Minute")) {
        ticks = result2 * 1000 * 60;
      } else if (selectedTimeUnit.equals("Hour")) {
        ticks = result2 * 1000 * 60 * 60;
      } else if (selectedTimeUnit.equals("Day")) {
        ticks = result2 * 1000 * 60 * 60 * 24;
      } else if (selectedTimeUnit.equals("Week")) {
        ticks = result2 * 1000 * 60 * 60 * 24 * 7;
      } else if (selectedTimeUnit.equals("Month")) {
        ticks = result2 * 1000 * 60 * 60 * 24 * 30;
      } else if (selectedTimeUnit.equals("Year")) {
        ticks = result2 * 1000 * 60 * 60 * 24 * 365;
      }
      final long expire = System.currentTimeMillis() + ticks;
      if (selectedMuteType.toLowerCase().contains("name")) {
        controller.muteUsername(clickedOn, new Date(expire));
      }
      if (selectedMuteType.toLowerCase().contains("mac")) {
        controller.muteMac(clickedOn, new Date(expire));
      }
    }));
    rVal.add(SwingAction.of("Quick Mute", e -> {
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
        final long ticks = resultMuteLengthInMinutes * 1000 * 60;
        final long expire = System.currentTimeMillis() + ticks;
        controller.muteUsername(clickedOn, new Date(expire));
        controller.muteMac(clickedOn, new Date(expire));
      }
    }));
    rVal.add(SwingAction.of("Show player information", e -> {
      final String text = controller.getInformationOn(clickedOn);
      final JTextPane textPane = new JTextPane();
      textPane.setEditable(false);
      textPane.setText(text);
      JOptionPane.showMessageDialog(null, textPane, "Player Info", JOptionPane.INFORMATION_MESSAGE);
    }));
    return rVal;

  }

  private boolean confirm(final String question) {
    final int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(this), question, "Question",
        JOptionPane.OK_CANCEL_OPTION);
    return rVal == JOptionPane.OK_OPTION;
  }

  public LobbyClient getLobbyClient() {
    return m_client;
  }

  public void setShowChatTime(final boolean showTime) {
    if (m_chatMessagePanel != null) {
      m_chatMessagePanel.setShowTime(showTime);
    }
  }

  public void shutdown() {
    setVisible(false);
    dispose();
    MainFrame.getInstance().setVisible(true);
    m_client.getMessenger().shutDown();
    GameRunner.exitGameIfFinished();
  }

  private void connectionToServerLost() {
    EventThreadJOptionPane.showMessageDialog(LobbyFrame.this,
        "Connection to Server Lost.  Please close this instance and reconnect to the lobby.", "Connection Lost",
        JOptionPane.ERROR_MESSAGE, new CountDownLatchHandler(true));
  }
}
