package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.framework.GameProcess;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.lobby.client.login.LoginResult;
import games.strategy.engine.lobby.client.ui.action.ShowPlayersAction;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.lobby.common.GameDescription;
import org.triplea.swing.MouseListenerBuilder;
import org.triplea.swing.SwingAction;

class LobbyGamePanel extends JPanel {
  private static final long serialVersionUID = -2576314388949606337L;
  private final JFrame parent;
  private final JButton joinGameButton;
  private final LobbyGameTableModel gameTableModel;
  private final LoginResult loginResult;
  private final JTable gameTable;
  private final PlayerToLobbyConnection playerToLobbyConnection;

  LobbyGamePanel(
      final JFrame parent,
      final LoginResult loginResult,
      final LobbyGameTableModel lobbyGameTableModel,
      final PlayerToLobbyConnection playerToLobbyConnection) {
    this.parent = parent;
    this.loginResult = loginResult;
    this.gameTableModel = lobbyGameTableModel;
    this.playerToLobbyConnection = playerToLobbyConnection;

    final JButton hostGameButton = new JButton("Host Game");
    joinGameButton = new JButton("Join Game");

    gameTable =
        new JTable(gameTableModel) {
          @Override
          // Custom renderer to show 'bot' rows in italic font
          public Component prepareRenderer(
              final TableCellRenderer renderer, final int rowIndex, final int colIndex) {

            final Component component = super.prepareRenderer(renderer, rowIndex, colIndex);
            final GameDescription gameDescription =
                lobbyGameTableModel.get(convertRowIndexToModel(rowIndex));
            component.setFont(
                gameDescription.isBot()
                    ? UIManager.getDefaults().getFont("Table.font").deriveFont(Font.ITALIC)
                    : UIManager.getDefaults().getFont("Table.font"));
            return component;
          }
        };
    gameTable
        .getSelectionModel()
        .addListSelectionListener(
            e -> {
              final boolean selected = gameTable.getSelectedRow() >= 0;
              joinGameButton.setEnabled(selected);
            });
    gameTable.addMouseListener(
        new MouseListenerBuilder()
            .mouseClicked(this::mouseClicked)
            .mousePressed(this::mousePressed)
            .mouseReleased(this::mouseOnGamesList)
            .build());

    final TableRowSorter<LobbyGameTableModel> tableSorter = new TableRowSorter<>(gameTableModel);
    // by default, sort by host
    final int hostColumn = gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Host);
    tableSorter.setSortKeys(List.of(new RowSorter.SortKey(hostColumn, SortOrder.DESCENDING)));
    gameTable.setRowSorter(tableSorter);

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
    if (loginResult.isModerator()) {
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
    toolBar.add(hostGameButton);
    toolBar.add(joinGameButton);
    toolBar.setFloatable(false);
    add(toolBar, BorderLayout.SOUTH);

    hostGameButton.addActionListener(e -> hostGame());
    joinGameButton.addActionListener(e -> joinGame());
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

    List.of(
            SwingAction.of("Join Game", this::joinGame),
            SwingAction.of("Host Game", this::hostGame),
            ShowPlayersAction.builder()
                .parentWindow(parent)
                .gameIdSelection(
                    () ->
                        gameTableModel.getGameListingForRow(
                            gameTable.convertRowIndexToModel(gameTable.getSelectedRow())))
                .playerToLobbyConnection(playerToLobbyConnection)
                .build()
                .buildSwingAction())
        .forEach(menu::add);

    if (loginResult.isModerator()) {
      menu.addSeparator();
      List.of(
              SwingAction.of("Boot Game", e -> bootGame()),
              SwingAction.of("Shutdown", e -> shutdown()))
          .forEach(menu::add);
    }

    if (menu.getComponentCount() > 0) {
      menu.show(gameTable, mouseEvent.getX(), mouseEvent.getY());
    }
  }

  private void joinGame() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    // we sort the table, so get the correct index
    final GameDescription description =
        gameTableModel.get(gameTable.convertRowIndexToModel(selectedIndex));
    GameProcess.joinGame(description, loginResult.getUsername());
  }

  private void hostGame() {
    final ServerOptions options =
        new ServerOptions(
            JOptionPane.getFrameForComponent(this), loginResult.getUsername(), 3300, true);
    options.setLocationRelativeTo(JOptionPane.getFrameForComponent(this));
    options.setNameEditable(false);
    options.setVisible(true);
    if (!options.getOkPressed()) {
      return;
    }
    GameProcess.hostGame(
        options.getPort(),
        options.getName(),
        options.getComments(),
        options.getPassword(),
        ClientSetting.lobbyUri.getValueOrThrow());
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

    gameTableModel.bootGame(gameTable.convertRowIndexToModel(selectedIndex));
    JOptionPane.showMessageDialog(
        null, "The game you selected has been disconnected from the lobby.");
  }

  private void shutdown() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    final int result =
        JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to shutdown the selected game?",
            "Send Shutdown Signal?",
            JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }

    final String gameId =
        gameTableModel.getGameIdForRow(gameTable.convertRowIndexToModel(selectedIndex));
    playerToLobbyConnection.sendShutdownRequest(gameId);
    JOptionPane.showMessageDialog(null, "The game you selected was sent a shutdown signal");
  }
}
