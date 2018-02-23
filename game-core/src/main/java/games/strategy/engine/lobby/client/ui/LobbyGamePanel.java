package games.strategy.engine.lobby.client.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import com.google.common.base.Strings;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.lobby.server.AbstractModeratorController;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.lobby.server.IModeratorController;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.net.Node;
import games.strategy.ui.SwingAction;

class LobbyGamePanel extends JPanel {
  private static final long serialVersionUID = -2576314388949606337L;
  private final Messengers messengers;
  private JButton hostGame;
  private JButton joinGame;
  private JButton bootGame;
  private LobbyGameTableModel gameTableModel;
  private JTable gameTable;

  LobbyGamePanel(final Messengers messengers) {
    this.messengers = messengers;
    createComponents();
    layoutComponents();
    setupListeners();
    setWidgetActivation();
  }

  private void createComponents() {
    hostGame = new JButton("Host Game");
    joinGame = new JButton("Join Game");
    bootGame = new JButton("Boot Game");
    gameTableModel = new LobbyGameTableModel(messengers.getMessenger(), messengers.getChannelMessenger(),
        messengers.getRemoteMessenger());

    gameTable = new LobbyGameTable(gameTableModel);
    // only allow one row to be selected
    gameTable.setColumnSelectionAllowed(false);
    gameTable.setCellSelectionEnabled(false);
    gameTable.setRowSelectionAllowed(true);
    gameTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    // these should add up to 700 at most
    gameTable.getColumnModel().getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Players))
        .setPreferredWidth(42);
    gameTable.getColumnModel().getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Round))
        .setPreferredWidth(40);
    gameTable.getColumnModel().getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.P))
        .setPreferredWidth(12);
    gameTable.getColumnModel().getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.B))
        .setPreferredWidth(12);
    gameTable.getColumnModel().getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.GV))
        .setPreferredWidth(32);
    gameTable.getColumnModel().getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.EV))
        .setPreferredWidth(42);
    gameTable.getColumnModel().getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Started))
        .setPreferredWidth(55);
    gameTable.getColumnModel().getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Status))
        .setPreferredWidth(112);
    gameTable.getColumnModel().getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Name))
        .setPreferredWidth(156);
    gameTable.getColumnModel().getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Comments))
        .setPreferredWidth(130);
    gameTable.getColumnModel().getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Host))
        .setPreferredWidth(67);
  }

  private void layoutComponents() {
    final JScrollPane scroll = new JScrollPane(gameTable);
    setLayout(new BorderLayout());
    add(scroll, BorderLayout.CENTER);
    final JToolBar toolBar = new JToolBar();
    toolBar.add(hostGame);
    toolBar.add(joinGame);
    if (isAdmin()) {
      toolBar.add(bootGame);
    }
    toolBar.setFloatable(false);
    add(toolBar, BorderLayout.SOUTH);
  }

  boolean isAdmin() {
    return ((IModeratorController) messengers.getRemoteMessenger()
        .getRemote(AbstractModeratorController.getModeratorControllerName())).isAdmin();
  }

  private void setupListeners() {
    hostGame.addActionListener(e -> hostGame());
    joinGame.addActionListener(e -> joinGame());
    bootGame.addActionListener(e -> bootGame());
    gameTable.getSelectionModel().addListSelectionListener(e -> setWidgetActivation());
    gameTable.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        if (e.getClickCount() == 2) {
          joinGame();
        }
        mouseOnGamesList(e);
      }

      @Override
      public void mousePressed(final MouseEvent e) {
        // right clicks do not 'select' a row by default. so force a row selection at the mouse point.
        final int r = gameTable.rowAtPoint(e.getPoint());
        if ((r >= 0) && (r < gameTable.getRowCount())) {
          gameTable.setRowSelectionInterval(r, r);
        } else {
          gameTable.clearSelection();
        }
        mouseOnGamesList(e);
      }

      @Override
      public void mouseReleased(final MouseEvent e) {
        mouseOnGamesList(e);
      }

      @Override
      public void mouseEntered(final MouseEvent e) {} // ignore

      @Override
      public void mouseExited(final MouseEvent e) {} // ignore
    });
  }

  private void mouseOnGamesList(final MouseEvent e) {
    if (!e.isPopupTrigger()) {
      return;
    }
    if (!SwingUtilities.isRightMouseButton(e)) {
      return;
    }
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    // we sort the table, so get the correct index
    final GameDescription description = gameTableModel.get(gameTable.convertRowIndexToModel(selectedIndex));
    final JPopupMenu menu = new JPopupMenu();
    boolean hasActions = false;
    for (final Action a : getGamesListRightClickActions(description)) {
      if (a == null) {
        continue;
      }
      hasActions = true;
      menu.add(a);
    }
    if (hasActions) {
      menu.show(gameTable, e.getX(), e.getY());
    }
  }

  private List<Action> getGamesListRightClickActions(final GameDescription description) {
    final List<Action> actions = new ArrayList<>();
    actions.add(getJoinGameAction());
    actions.add(getHostGameAction());
    if (isAdmin()) {
      actions.add(getHostSupportInfoAction(description));
      actions.add(getHostInfoAction());
      actions.add(getChatLogOfHeadlessHostBotAction(description));
      actions.add(getMutePlayerHeadlessHostBotAction(description));
      actions.add(getBootPlayerHeadlessHostBotAction(description));
      actions.add(getBanPlayerHeadlessHostBotAction(description));
      actions.add(getStopGameHeadlessHostBotAction(description));
      actions.add(getShutDownHeadlessHostBotAction(description));
      actions.add(getBootGameAction());
    }
    return actions;
  }

  private static Action getHostSupportInfoAction(final GameDescription description) {
    final String supportEmail = (description == null) ? "" : Strings.nullToEmpty(description.getBotSupportEmail());
    if (supportEmail.isEmpty()) {
      return null;
    }
    final String text = "Support Email for this Host is as follows. "
        + "\n(Please copy the email address below and manually email them ONLY if something is seriously "
        + "\nwrong with the host, like it needs to be restarted because it is down and not working at all.) "
        + "\n\nEmail: \n" + supportEmail;
    return SwingAction.of("Show Host Support Information/Email", e -> {
      final JTextPane textPane = new JTextPane();
      textPane.setEditable(false);
      textPane.setText(text);
      JOptionPane.showMessageDialog(null, textPane, "Host Support Info", JOptionPane.INFORMATION_MESSAGE);
    });
  }

  private Action getJoinGameAction() {
    return SwingAction.of("Join Game", e -> joinGame());
  }

  private Action getHostGameAction() {
    return SwingAction.of("Host Game", e -> hostGame());
  }

  private Action getBootGameAction() {
    return SwingAction.of("Boot Game", e -> bootGame());
  }

  private Action getHostInfoAction() {
    return SwingAction.of("Host Information", e -> getHostInfo());
  }

  private Action getChatLogOfHeadlessHostBotAction(final GameDescription description) {
    final String supportEmail = (description == null)
        ? ""
        : ((description.getBotSupportEmail() == null)
            ? ""
            : description.getBotSupportEmail());
    if (supportEmail.length() == 0) {
      return null;
    }
    return SwingAction.of("Get Chat Log Of Headless Host Bot", e -> getChatLogOfHeadlessHostBot());
  }

  private Action getMutePlayerHeadlessHostBotAction(final GameDescription description) {
    final String supportEmail = (description == null)
        ? ""
        : ((description.getBotSupportEmail() == null)
            ? ""
            : description.getBotSupportEmail());
    if (supportEmail.length() == 0) {
      return null;
    }
    return SwingAction.of("Mute Player In Headless Host Bot", e -> mutePlayerInHeadlessHostBot());
  }

  private Action getBootPlayerHeadlessHostBotAction(final GameDescription description) {
    final String supportEmail = (description == null)
        ? ""
        : ((description.getBotSupportEmail() == null) ? "" : description.getBotSupportEmail());
    if (supportEmail.length() == 0) {
      return null;
    }
    return SwingAction.of("Boot Player In Headless Host Bot", e -> bootPlayerInHeadlessHostBot());
  }

  private Action getBanPlayerHeadlessHostBotAction(final GameDescription description) {
    final String supportEmail = (description == null)
        ? ""
        : ((description.getBotSupportEmail() == null)
            ? ""
            : description.getBotSupportEmail());
    if (supportEmail.length() == 0) {
      return null;
    }
    return SwingAction.of("Ban Player In Headless Host Bot", e -> banPlayerInHeadlessHostBot());
  }

  private Action getShutDownHeadlessHostBotAction(final GameDescription description) {
    final String supportEmail = (description == null)
        ? ""
        : ((description.getBotSupportEmail() == null)
            ? ""
            : description.getBotSupportEmail());
    if (supportEmail.length() == 0) {
      return null;
    }
    return SwingAction.of("Remote Shutdown Headless Host Bot", e -> shutDownHeadlessHostBot());
  }

  private Action getStopGameHeadlessHostBotAction(final GameDescription description) {
    final String supportEmail = (description == null)
        ? ""
        : ((description.getBotSupportEmail() == null)
            ? ""
            : description.getBotSupportEmail());
    if (supportEmail.length() == 0) {
      return null;
    }
    return SwingAction.of("Remote Stop Game Headless Host Bot", e -> stopGameHeadlessHostBot());
  }

  private void joinGame() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    // we sort the table, so get the correct index
    final GameDescription description = gameTableModel.get(gameTable.convertRowIndexToModel(selectedIndex));
    GameRunner.joinGame(description, messengers, getParent());
  }

  private void hostGame() {
    final ServerOptions options = new ServerOptions(JOptionPane.getFrameForComponent(this),
        messengers.getMessenger().getLocalNode().getName(), 3300, true);
    options.setLocationRelativeTo(JOptionPane.getFrameForComponent(this));
    options.setNameEditable(false);
    options.setVisible(true);
    if (!options.getOkPressed()) {
      return;
    }
    GameRunner.hostGame(options.getPort(), options.getName(), options.getComments(), options.getPassword(),
        messengers);
  }

  private void bootGame() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to disconnect the selected game?",
        "Remove Game From Lobby", JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = (IModeratorController) messengers.getRemoteMessenger()
        .getRemote(AbstractModeratorController.getModeratorControllerName());
    controller.boot(lobbyWatcherNode);
    JOptionPane.showMessageDialog(null, "The game you selected has been disconnected from the lobby.");
  }

  private void getHostInfo() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = (IModeratorController) messengers.getRemoteMessenger()
        .getRemote(AbstractModeratorController.getModeratorControllerName());
    final String text = controller.getInformationOn(lobbyWatcherNode);
    final String connections = controller.getHostConnections(lobbyWatcherNode);
    final JTextPane textPane = new JTextPane();
    textPane.setEditable(false);
    textPane.setText(text + "\n\n" + connections);
    JOptionPane.showMessageDialog(null, textPane, "Player Info", JOptionPane.INFORMATION_MESSAGE);
  }

  private void getChatLogOfHeadlessHostBot() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final int result =
        JOptionPane.showConfirmDialog(null, "Are you sure you want to perform a remote get chat log this host?",
            "Remote Get Chat Log Headless Host Bot", JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    // we sort the table, so get the correct index
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = (IModeratorController) messengers.getRemoteMessenger()
        .getRemote(AbstractModeratorController.getModeratorControllerName());
    final JLabel label = new JLabel("Enter Host Remote Access Password, (Leave blank for no password).");
    final JPasswordField passwordField = new JPasswordField();
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(label, BorderLayout.NORTH);
    panel.add(passwordField, BorderLayout.CENTER);
    final int selectedOption = JOptionPane.showOptionDialog(getTopLevelAncestor(), panel,
        "Host Remote Access Password?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    if ((selectedOption != JOptionPane.OK_OPTION) || (passwordField.getPassword() == null)) {
      return;
    }
    final String password = new String(passwordField.getPassword());
    final String salt = controller.getHeadlessHostBotSalt(lobbyWatcherNode);
    final String hashedPassword = md5Crypt(password, salt);
    final String response = controller.getChatLogHeadlessHostBot(lobbyWatcherNode, hashedPassword, salt);
    final JTextPane textPane = new JTextPane();
    textPane.setEditable(false);
    textPane.setText((response == null) ? "Failed to get chat log!" : response);
    textPane.setCaretPosition(textPane.getText().length());
    final JScrollPane scroll = new JScrollPane(textPane);
    final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
    final int availWidth = screenResolution.width - 100;
    final int availHeight = screenResolution.height - 140;
    scroll.setPreferredSize(new Dimension(Math.min(availWidth, scroll.getPreferredSize().width),
        Math.min(availHeight, scroll.getPreferredSize().height)));
    JOptionPane.showMessageDialog(null, scroll, "Bot Chat Log", JOptionPane.INFORMATION_MESSAGE);
  }

  private static String md5Crypt(final String value, final String salt) {
    return games.strategy.util.Md5Crypt.crypt(value, salt);
  }

  private INode getLobbyWatcherNodeForTableRow(final int selectedIndex) {
    final GameDescription description = gameTableModel.get(gameTable.convertRowIndexToModel(selectedIndex));
    final String hostedByName = description.getHostedBy().getName();
    return new Node(
        (hostedByName.endsWith("_" + InGameLobbyWatcher.LOBBY_WATCHER_NAME) ? hostedByName
            : (hostedByName + "_" + InGameLobbyWatcher.LOBBY_WATCHER_NAME)),
        description.getHostedBy().getAddress(), description.getHostedBy().getPort());
  }

  private void mutePlayerInHeadlessHostBot() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final int result =
        JOptionPane.showConfirmDialog(null, "Are you sure you want to perform a remote mute player on this host?",
            "Remote Player Mute Headless Host Bot", JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    final String playerToBeMuted = JOptionPane.showInputDialog(getTopLevelAncestor(), "Player Name To Be Muted?",
        "Player Name To Be Muted?", JOptionPane.QUESTION_MESSAGE);
    if (playerToBeMuted == null) {
      return;
    }
    final Object minutes = JOptionPane.showInputDialog(getTopLevelAncestor(),
        "Minutes to Mute for?  (between 0 and 2880, choose zero to unmute [works only if players is in the host])",
        "Minutes to Mute for?", JOptionPane.QUESTION_MESSAGE, null, null, 10);
    if (minutes == null) {
      return;
    }
    final int min;
    try {
      min = Math.max(0, Math.min(60 * 24 * 2, Integer.parseInt((String) minutes)));
    } catch (final NumberFormatException e) {
      return;
    }
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = (IModeratorController) messengers.getRemoteMessenger()
        .getRemote(AbstractModeratorController.getModeratorControllerName());
    final JLabel label = new JLabel("Enter Host Remote Access Password, (Leave blank for no password).");
    final JPasswordField passwordField = new JPasswordField();
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(label, BorderLayout.NORTH);
    panel.add(passwordField, BorderLayout.CENTER);
    final int selectedOption = JOptionPane.showOptionDialog(getTopLevelAncestor(), panel,
        "Host Remote Access Password?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    if ((selectedOption != JOptionPane.OK_OPTION) || (passwordField.getPassword() == null)) {
      return;
    }
    final String password = new String(passwordField.getPassword());
    final String salt = controller.getHeadlessHostBotSalt(lobbyWatcherNode);
    final String hashedPassword = md5Crypt(password, salt);
    final String response =
        controller.mutePlayerHeadlessHostBot(lobbyWatcherNode, playerToBeMuted, min, hashedPassword, salt);
    JOptionPane.showMessageDialog(null, ((response == null)
        ? ("Successfully attempted to mute player (" + playerToBeMuted + ") on host")
        : ("Failed: " + response)));
  }

  private void bootPlayerInHeadlessHostBot() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final int result =
        JOptionPane.showConfirmDialog(null, "Are you sure you want to perform a remote boot player on this host?",
            "Remote Player Boot Headless Host Bot", JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    final String playerToBeBooted = JOptionPane.showInputDialog(getTopLevelAncestor(), "Player Name To Be Booted?",
        "Player Name To Be Booted?", JOptionPane.QUESTION_MESSAGE);
    if (playerToBeBooted == null) {
      return;
    }
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = (IModeratorController) messengers.getRemoteMessenger()
        .getRemote(AbstractModeratorController.getModeratorControllerName());
    final JLabel label = new JLabel("Enter Host Remote Access Password, (Leave blank for no password).");
    final JPasswordField passwordField = new JPasswordField();
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(label, BorderLayout.NORTH);
    panel.add(passwordField, BorderLayout.CENTER);
    final int selectedOption = JOptionPane.showOptionDialog(getTopLevelAncestor(), panel,
        "Host Remote Access Password?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    if ((selectedOption != JOptionPane.OK_OPTION) || (passwordField.getPassword() == null)) {
      return;
    }
    final String password = new String(passwordField.getPassword());
    final String salt = controller.getHeadlessHostBotSalt(lobbyWatcherNode);
    final String hashedPassword = md5Crypt(password, salt);
    final String response =
        controller.bootPlayerHeadlessHostBot(lobbyWatcherNode, playerToBeBooted, hashedPassword, salt);
    JOptionPane.showMessageDialog(null, ((response == null)
        ? ("Successfully attempted to boot player (" + playerToBeBooted + ") on host")
        : ("Failed: " + response)));
  }

  private void banPlayerInHeadlessHostBot() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final int result = JOptionPane.showConfirmDialog(null,
        "Are you sure you want to perform a (permanent) remote ban player on this host?",
        "Remote Player Ban Headless Host Bot", JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    final String playerToBeBanned = JOptionPane.showInputDialog(getTopLevelAncestor(), "Player Name To Be Banned?",
        "Player Name To Be Banned?", JOptionPane.QUESTION_MESSAGE);
    if (playerToBeBanned == null) {
      return;
    }
    final Object hours = JOptionPane.showInputDialog(getTopLevelAncestor(),
        "Hours to Ban for?  (between 0 and 720, this is permanent and only a restart of the host will undo it!)",
        "Hours to Ban for?", JOptionPane.QUESTION_MESSAGE, null, null, 24);
    if (hours == null) {
      return;
    }
    final int hrs;
    try {
      hrs = Math.max(0, Math.min(24 * 30, Integer.parseInt((String) hours)));
    } catch (final NumberFormatException e) {
      return;
    }
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = (IModeratorController) messengers.getRemoteMessenger()
        .getRemote(AbstractModeratorController.getModeratorControllerName());
    final JLabel label = new JLabel("Enter Host Remote Access Password, (Leave blank for no password).");
    final JPasswordField passwordField = new JPasswordField();
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(label, BorderLayout.NORTH);
    panel.add(passwordField, BorderLayout.CENTER);
    final int selectedOption = JOptionPane.showOptionDialog(getTopLevelAncestor(), panel,
        "Host Remote Access Password?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    if ((selectedOption != JOptionPane.OK_OPTION) || (passwordField.getPassword() == null)) {
      return;
    }
    final String password = new String(passwordField.getPassword());
    final String salt = controller.getHeadlessHostBotSalt(lobbyWatcherNode);
    final String hashedPassword = md5Crypt(password, salt);
    final String response =
        controller.banPlayerHeadlessHostBot(lobbyWatcherNode, playerToBeBanned, hrs, hashedPassword, salt);
    JOptionPane.showMessageDialog(null, ((response == null)
        ? ("Successfully attempted banned player (" + playerToBeBanned + ") on host")
        : ("Failed: " + response)));
  }

  private void stopGameHeadlessHostBot() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final int result =
        JOptionPane.showConfirmDialog(null, "Are you sure you want to perform a remote stop game on this host?",
            "Remote Stopgame Headless Host Bot", JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = (IModeratorController) messengers.getRemoteMessenger()
        .getRemote(AbstractModeratorController.getModeratorControllerName());
    final JLabel label = new JLabel("Enter Host Remote Access Password, (Leave blank for no password).");
    final JPasswordField passwordField = new JPasswordField();
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(label, BorderLayout.NORTH);
    panel.add(passwordField, BorderLayout.CENTER);
    final int selectedOption = JOptionPane.showOptionDialog(getTopLevelAncestor(), panel,
        "Host Remote Access Password?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    if ((selectedOption != JOptionPane.OK_OPTION) || (passwordField.getPassword() == null)) {
      return;
    }
    final String password = new String(passwordField.getPassword());
    final String salt = controller.getHeadlessHostBotSalt(lobbyWatcherNode);
    final String hashedPassword = md5Crypt(password, salt);
    final String response = controller.stopGameHeadlessHostBot(lobbyWatcherNode, hashedPassword, salt);
    JOptionPane.showMessageDialog(null,
        ((response == null) ? "Successfully attempted stop of current game on host" : ("Failed: " + response)));
  }

  private void shutDownHeadlessHostBot() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final int result = JOptionPane.showConfirmDialog(null,
        "Are you sure you want to perform a remote shutdown of this host? \n\nYou MUST email the host's owner FIRST!!",
        "Remote Shutdown Headless Host Bot", JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = (IModeratorController) messengers.getRemoteMessenger()
        .getRemote(AbstractModeratorController.getModeratorControllerName());
    final JLabel label = new JLabel("Enter Host Remote Access Password, (Leave blank for no password).");
    final JPasswordField passwordField = new JPasswordField();
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(label, BorderLayout.NORTH);
    panel.add(passwordField, BorderLayout.CENTER);
    final int selectedOption = JOptionPane.showOptionDialog(getTopLevelAncestor(), panel,
        "Host Remote Access Password?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    if ((selectedOption != JOptionPane.OK_OPTION) || (passwordField.getPassword() == null)) {
      return;
    }
    final String password = new String(passwordField.getPassword());
    final String salt = controller.getHeadlessHostBotSalt(lobbyWatcherNode);
    final String hashedPassword = md5Crypt(password, salt);
    final String response = controller.shutDownHeadlessHostBot(lobbyWatcherNode, hashedPassword, salt);
    JOptionPane.showMessageDialog(null,
        ((response == null) ? "Successfully attempted to shut down host" : ("Failed: " + response)));
  }

  private void setWidgetActivation() {
    final boolean selected = gameTable.getSelectedRow() >= 0;
    joinGame.setEnabled(selected);
  }
}
