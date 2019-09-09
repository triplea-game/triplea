package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.net.INode;
import games.strategy.net.Node;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Collection;
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
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.common.GameDescription;
import org.triplea.lobby.common.IModeratorController;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.swing.SwingAction;

class LobbyGamePanel extends JPanel {
  private static final long serialVersionUID = -2576314388949606337L;
  private JButton hostGame;
  private JButton joinGame;
  private JButton bootGame;
  private LobbyGameTableModel gameTableModel;
  private final LobbyClient lobbyClient;
  private JTable gameTable;

  LobbyGamePanel(final LobbyClient lobbyClient, final LobbyGameTableModel lobbyGameTableModel) {
    this.lobbyClient = lobbyClient;
    this.gameTableModel = lobbyGameTableModel;

    hostGame = new JButton("Host Game");
    joinGame = new JButton("Join Game");
    bootGame = new JButton("Boot Game");

    gameTable = new LobbyGameTable(gameTableModel);
    // only allow one row to be selected
    gameTable.setColumnSelectionAllowed(false);
    gameTable.setCellSelectionEnabled(false);
    gameTable.setRowSelectionAllowed(true);
    gameTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    // these should add up to 700 at most
    gameTable
        .getColumnModel()
        .getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Players))
        .setPreferredWidth(42);
    gameTable
        .getColumnModel()
        .getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Round))
        .setPreferredWidth(40);
    gameTable
        .getColumnModel()
        .getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.P))
        .setPreferredWidth(12);
    gameTable
        .getColumnModel()
        .getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.GV))
        .setPreferredWidth(32);
    if (lobbyClient.isAdmin()) {
      gameTable
          .getColumnModel()
          .getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Started))
          .setPreferredWidth(55);
    }
    gameTable
        .getColumnModel()
        .getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Status))
        .setPreferredWidth(112);
    gameTable
        .getColumnModel()
        .getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Name))
        .setPreferredWidth(156);
    gameTable
        .getColumnModel()
        .getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Comments))
        .setPreferredWidth(160);
    gameTable
        .getColumnModel()
        .getColumn(gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Host))
        .setPreferredWidth(67);

    final JScrollPane scroll = new JScrollPane(gameTable);
    setLayout(new BorderLayout());
    add(scroll, BorderLayout.CENTER);
    final JToolBar toolBar = new JToolBar();
    toolBar.add(hostGame);
    toolBar.add(joinGame);
    if (lobbyClient.isAdmin()) {
      toolBar.add(bootGame);
    }
    toolBar.setFloatable(false);
    add(toolBar, BorderLayout.SOUTH);

    hostGame.addActionListener(e -> hostGame());
    joinGame.addActionListener(e -> joinGame());
    bootGame.addActionListener(e -> bootGame());
    gameTable
        .getSelectionModel()
        .addListSelectionListener(
            e -> {
              final boolean selected = gameTable.getSelectedRow() >= 0;
              joinGame.setEnabled(selected);
            });
    gameTable.addMouseListener(
        new MouseListener() {
          @Override
          public void mouseClicked(final MouseEvent e) {
            if (e.getClickCount() == 2) {
              joinGame();
            }
            mouseOnGamesList(e);
          }

          @Override
          public void mousePressed(final MouseEvent e) {
            // right clicks do not 'select' a row by default. so force a row selection at the mouse
            // point.
            final int r = gameTable.rowAtPoint(e.getPoint());
            if (r >= 0 && r < gameTable.getRowCount()) {
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

    final JPopupMenu menu = new JPopupMenu();

    getUserGamesListContextActions().forEach(menu::add);

    if (lobbyClient.isAdmin()) {
      final Collection<Action> generalAdminActions = getGeneralAdminGamesListContextActions();
      if (!generalAdminActions.isEmpty()) {
        menu.addSeparator();
        generalAdminActions.forEach(menu::add);
      }

      final GameDescription gameDescription =
          gameTableModel.get(gameTable.convertRowIndexToModel(selectedIndex));
      if (gameDescription.isBot()) {
        final Collection<Action> botAdminActions = getBotAdminGamesListContextActions();
        if (!botAdminActions.isEmpty()) {
          menu.addSeparator();
          botAdminActions.forEach(menu::add);
        }
      }
    }

    if (menu.getComponentCount() > 0) {
      menu.show(gameTable, e.getX(), e.getY());
    }
  }

  private Collection<Action> getUserGamesListContextActions() {
    return Arrays.asList(
        SwingAction.of("Join Game", e -> joinGame()), SwingAction.of("Host Game", e -> hostGame()));
  }

  private Collection<Action> getGeneralAdminGamesListContextActions() {
    return Arrays.asList(
        SwingAction.of("Host Information", e -> getHostInfo()),
        SwingAction.of("Boot Game", e -> bootGame()));
  }

  private Collection<Action> getBotAdminGamesListContextActions() {
    return Arrays.asList(
        SwingAction.of("Get Chat Log Of Headless Host Bot", e -> getChatLogOfHeadlessHostBot()),
        SwingAction.of("Boot Player In Headless Host Bot", e -> bootPlayerInHeadlessHostBot()),
        SwingAction.of("Ban Player In Headless Host Bot", e -> banPlayerInHeadlessHostBot()),
        SwingAction.of("Remote Stop Game Headless Host Bot", e -> stopGameHeadlessHostBot()),
        SwingAction.of("Remote Shutdown Headless Host Bot", e -> shutDownHeadlessHostBot()));
  }

  private void joinGame() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    // we sort the table, so get the correct index
    final GameDescription description =
        gameTableModel.get(gameTable.convertRowIndexToModel(selectedIndex));
    GameRunner.joinGame(description, lobbyClient.getPlayerName());
  }

  private void hostGame() {
    final ServerOptions options =
        new ServerOptions(
            JOptionPane.getFrameForComponent(this), lobbyClient.getPlayerName(), 3300, true);
    options.setLocationRelativeTo(JOptionPane.getFrameForComponent(this));
    options.setNameEditable(false);
    options.setVisible(true);
    if (!options.getOkPressed()) {
      return;
    }
    GameRunner.hostGame(
        options.getPort(),
        options.getName(),
        options.getComments(),
        options.getPassword(),
        lobbyClient.getLobbyHostAddress(),
        lobbyClient.getLobbyPort());
  }

  private void bootGame() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final int result =
        JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to disconnect the selected game?",
            "Remove Game From Lobby",
            JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = lobbyClient.getModeratorController();
    controller.boot(lobbyWatcherNode);
    JOptionPane.showMessageDialog(
        null, "The game you selected has been disconnected from the lobby.");
  }

  private void getHostInfo() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = lobbyClient.getModeratorController();
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
        JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to perform a remote get chat log this host?",
            "Remote Get Chat Log Headless Host Bot",
            JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    // we sort the table, so get the correct index
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = lobbyClient.getModeratorController();
    final JLabel label =
        new JLabel("Enter Host Remote Access Password, (Leave blank for no password).");
    final JPasswordField passwordField = new JPasswordField();
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(label, BorderLayout.NORTH);
    panel.add(passwordField, BorderLayout.CENTER);
    final int selectedOption =
        JOptionPane.showOptionDialog(
            getTopLevelAncestor(),
            panel,
            "Host Remote Access Password?",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            null);
    if (selectedOption != JOptionPane.OK_OPTION || passwordField.getPassword() == null) {
      return;
    }
    final String password = new String(passwordField.getPassword());
    final String salt = controller.getHeadlessHostBotSalt(lobbyWatcherNode);
    final String hashedPassword = hashPassword(password, salt);
    final String response =
        controller.getChatLogHeadlessHostBot(lobbyWatcherNode, hashedPassword, salt);
    final JTextPane textPane = new JTextPane();
    textPane.setEditable(false);
    textPane.setText(response == null ? "Failed to get chat log!" : response);
    textPane.setCaretPosition(textPane.getText().length());
    final JScrollPane scroll = new JScrollPane(textPane);
    final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
    final int availWidth = screenResolution.width - 100;
    final int availHeight = screenResolution.height - 140;
    scroll.setPreferredSize(
        new Dimension(
            Math.min(availWidth, scroll.getPreferredSize().width),
            Math.min(availHeight, scroll.getPreferredSize().height)));
    JOptionPane.showMessageDialog(null, scroll, "Bot Chat Log", JOptionPane.INFORMATION_MESSAGE);
  }

  private static String hashPassword(final String password, final String salt) {
    return BCrypt.hashpw(password, salt);
  }

  private INode getLobbyWatcherNodeForTableRow(final int selectedIndex) {
    final GameDescription description =
        gameTableModel.get(gameTable.convertRowIndexToModel(selectedIndex));
    final String hostedByName = description.getHostedBy().getName();
    return new Node(
        (hostedByName.endsWith("_" + LobbyConstants.LOBBY_WATCHER_NAME)
            ? hostedByName
            : hostedByName + "_" + LobbyConstants.LOBBY_WATCHER_NAME),
        description.getHostedBy().getAddress(),
        description.getHostedBy().getPort());
  }

  private void bootPlayerInHeadlessHostBot() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final int result =
        JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to perform a remote boot player on this host?",
            "Remote Player Boot Headless Host Bot",
            JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    final String playerToBeBooted =
        JOptionPane.showInputDialog(
            getTopLevelAncestor(),
            "Player Name To Be Booted?",
            "Player Name To Be Booted?",
            JOptionPane.QUESTION_MESSAGE);
    if (playerToBeBooted == null) {
      return;
    }
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = lobbyClient.getModeratorController();
    final JLabel label =
        new JLabel("Enter Host Remote Access Password, (Leave blank for no password).");
    final JPasswordField passwordField = new JPasswordField();
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(label, BorderLayout.NORTH);
    panel.add(passwordField, BorderLayout.CENTER);
    final int selectedOption =
        JOptionPane.showOptionDialog(
            getTopLevelAncestor(),
            panel,
            "Host Remote Access Password?",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            null);
    if (selectedOption != JOptionPane.OK_OPTION || passwordField.getPassword() == null) {
      return;
    }
    final String password = new String(passwordField.getPassword());
    final String salt = controller.getHeadlessHostBotSalt(lobbyWatcherNode);
    final String hashedPassword = hashPassword(password, salt);
    final String response =
        controller.bootPlayerHeadlessHostBot(
            lobbyWatcherNode, playerToBeBooted, hashedPassword, salt);
    JOptionPane.showMessageDialog(
        null,
        (response == null
            ? "Successfully attempted to boot player (" + playerToBeBooted + ") on host"
            : "Failed: " + response));
  }

  private void banPlayerInHeadlessHostBot() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final int result =
        JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to perform a (permanent) remote ban player on this host?",
            "Remote Player Ban Headless Host Bot",
            JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    final String playerToBeBanned =
        JOptionPane.showInputDialog(
            getTopLevelAncestor(),
            "Player Name To Be Banned?",
            "Player Name To Be Banned?",
            JOptionPane.QUESTION_MESSAGE);
    if (playerToBeBanned == null) {
      return;
    }
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = lobbyClient.getModeratorController();
    final JLabel label =
        new JLabel("Enter Host Remote Access Password, (Leave blank for no password).");
    final JPasswordField passwordField = new JPasswordField();
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(label, BorderLayout.NORTH);
    panel.add(passwordField, BorderLayout.CENTER);
    final int selectedOption =
        JOptionPane.showOptionDialog(
            getTopLevelAncestor(),
            panel,
            "Host Remote Access Password?",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            null);
    if (selectedOption != JOptionPane.OK_OPTION || passwordField.getPassword() == null) {
      return;
    }
    final String password = new String(passwordField.getPassword());
    final String salt = controller.getHeadlessHostBotSalt(lobbyWatcherNode);
    final String hashedPassword = hashPassword(password, salt);
    final String response =
        controller.banPlayerHeadlessHostBot(
            lobbyWatcherNode, playerToBeBanned, hashedPassword, salt);
    JOptionPane.showMessageDialog(
        null,
        (response == null
            ? "Successfully banned player (" + playerToBeBanned + ") on host"
            : "Failed: " + response));
  }

  private void stopGameHeadlessHostBot() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final int result =
        JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to perform a remote stop game on this host?",
            "Remote Stopgame Headless Host Bot",
            JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = lobbyClient.getModeratorController();
    final JLabel label =
        new JLabel("Enter Host Remote Access Password, (Leave blank for no password).");
    final JPasswordField passwordField = new JPasswordField();
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(label, BorderLayout.NORTH);
    panel.add(passwordField, BorderLayout.CENTER);
    final int selectedOption =
        JOptionPane.showOptionDialog(
            getTopLevelAncestor(),
            panel,
            "Host Remote Access Password?",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            null);
    if (selectedOption != JOptionPane.OK_OPTION || passwordField.getPassword() == null) {
      return;
    }
    final String password = new String(passwordField.getPassword());
    final String salt = controller.getHeadlessHostBotSalt(lobbyWatcherNode);
    final String hashedPassword = hashPassword(password, salt);
    final String response =
        controller.stopGameHeadlessHostBot(lobbyWatcherNode, hashedPassword, salt);
    JOptionPane.showMessageDialog(
        null,
        (response == null ? "Successfully stopped current game on host" : "Failed: " + response));
  }

  private void shutDownHeadlessHostBot() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final int result =
        JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to perform a remote shutdown of this host? \n\n"
                + "You MUST email the host's owner FIRST!!",
            "Remote Shutdown Headless Host Bot",
            JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    final INode lobbyWatcherNode = getLobbyWatcherNodeForTableRow(selectedIndex);
    final IModeratorController controller = lobbyClient.getModeratorController();
    final JLabel label =
        new JLabel("Enter Host Remote Access Password, (Leave blank for no password).");
    final JPasswordField passwordField = new JPasswordField();
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(label, BorderLayout.NORTH);
    panel.add(passwordField, BorderLayout.CENTER);
    final int selectedOption =
        JOptionPane.showOptionDialog(
            getTopLevelAncestor(),
            panel,
            "Host Remote Access Password?",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            null);
    if (selectedOption != JOptionPane.OK_OPTION || passwordField.getPassword() == null) {
      return;
    }
    final String password = new String(passwordField.getPassword());
    final String salt = controller.getHeadlessHostBotSalt(lobbyWatcherNode);
    final String hashedPassword = hashPassword(password, salt);
    final String response =
        controller.shutDownHeadlessHostBot(lobbyWatcherNode, hashedPassword, salt);
    JOptionPane.showMessageDialog(
        null, (response == null ? "Host shut down successful" : "Failed: " + response));
  }
}
