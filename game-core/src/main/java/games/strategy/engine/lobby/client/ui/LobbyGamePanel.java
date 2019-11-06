package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import org.triplea.lobby.common.GameDescription;
import org.triplea.swing.MouseListenerBuilder;
import org.triplea.swing.SwingAction;

class LobbyGamePanel extends JPanel {
  private static final long serialVersionUID = -2576314388949606337L;
  private JButton joinGame;
  private LobbyGameTableModel gameTableModel;
  private final LobbyClient lobbyClient;
  private JTable gameTable;
  private final LobbyServerProperties lobbyServerProperties;

  LobbyGamePanel(
      final LobbyClient lobbyClient,
      final LobbyServerProperties lobbyServerProperties,
      final LobbyGameTableModel lobbyGameTableModel) {
    this.lobbyClient = lobbyClient;
    this.gameTableModel = lobbyGameTableModel;
    this.lobbyServerProperties = lobbyServerProperties;

    final JButton hostGame = new JButton("Host Game");
    joinGame = new JButton("Join Game");
    final JButton bootGame = new JButton("Boot Game");

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
    if (lobbyClient.isModerator()) {
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
    if (lobbyClient.isModerator()) {
      toolBar.add(bootGame);
    }
    toolBar.setFloatable(false);
    add(toolBar, BorderLayout.SOUTH);

    hostGame.addActionListener(e -> hostGame(lobbyServerProperties));
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
        new MouseListenerBuilder()
            .mouseClicked(this::mouseClicked)
            .mousePressed(this::mousePressed)
            .mouseReleased(this::mouseOnGamesList)
            .build());
  }

  private void mouseClicked(final MouseEvent mouseEvent) {
    if (mouseEvent.getClickCount() == 2) {
      joinGame();
    }
    mouseOnGamesList(mouseEvent);
  }

  private void mousePressed(final MouseEvent mouseEvent) {
    // right clicks do not 'select' a row by default. so force a row selection at the mouse
    // point.
    final int r = gameTable.rowAtPoint(mouseEvent.getPoint());
    if (r >= 0 && r < gameTable.getRowCount()) {
      gameTable.setRowSelectionInterval(r, r);
    } else {
      gameTable.clearSelection();
    }
    mouseOnGamesList(mouseEvent);
  }

  private void mouseOnGamesList(final MouseEvent mouseEvent) {
    if (!mouseEvent.isPopupTrigger()) {
      return;
    }
    if (!SwingUtilities.isRightMouseButton(mouseEvent)) {
      return;
    }
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }

    final JPopupMenu menu = new JPopupMenu();

    Arrays.asList(
            SwingAction.of("Join Game", this::joinGame),
            SwingAction.of("Host Game", () -> hostGame(lobbyServerProperties)))
        .forEach(menu::add);

    if (lobbyClient.isModerator()) {
      final Collection<Action> generalAdminActions = getGeneralAdminGamesListContextActions();
      if (!generalAdminActions.isEmpty()) {
        menu.addSeparator();
        generalAdminActions.forEach(menu::add);
      }
    }

    if (menu.getComponentCount() > 0) {
      menu.show(gameTable, mouseEvent.getX(), mouseEvent.getY());
    }
  }

  private Collection<Action> getGeneralAdminGamesListContextActions() {
    return Collections.singletonList(SwingAction.of("Boot Game", e -> bootGame()));
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

  private void hostGame(final LobbyServerProperties lobbyServerProperties) {
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
        lobbyServerProperties);
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

    final String gameId = gameTableModel.getGameIdForRow(selectedIndex);
    lobbyClient.getHttpLobbyClient().getModeratorLobbyClient().disconnectGame(gameId);
    JOptionPane.showMessageDialog(
        null, "The game you selected has been disconnected from the lobby.");
  }
}
