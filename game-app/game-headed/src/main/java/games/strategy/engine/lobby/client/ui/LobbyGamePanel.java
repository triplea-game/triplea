package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.framework.GameProcess;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.ui.action.FetchChatHistory;
import games.strategy.engine.lobby.client.ui.action.ShowPlayersAction;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import javax.swing.Action;
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
import org.triplea.lobby.common.GameDescription;
import org.triplea.swing.MouseListenerBuilder;
import org.triplea.swing.SwingAction;

class LobbyGamePanel extends JPanel {
  private static final long serialVersionUID = -2576314388949606337L;
  private final JFrame parent;
  private final JButton joinGame;
  private final LobbyGameTableModel gameTableModel;
  private final LobbyClient lobbyClient;
  private final URI lobbyUri;
  private final JTable gameTable;

  LobbyGamePanel(
      final JFrame parent,
      final LobbyClient lobbyClient,
      final URI lobbyUri,
      final LobbyGameTableModel lobbyGameTableModel) {
    this.parent = parent;
    this.lobbyClient = lobbyClient;
    this.gameTableModel = lobbyGameTableModel;
    this.lobbyUri = lobbyUri;

    final JButton hostGame = new JButton("Host Game");
    joinGame = new JButton("Join Game");

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
    toolBar.setFloatable(false);
    add(toolBar, BorderLayout.SOUTH);

    hostGame.addActionListener(e -> hostGame(lobbyUri));
    joinGame.addActionListener(e -> joinGame());
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

    List.of(
            SwingAction.of("Join Game", this::joinGame),
            SwingAction.of("Host Game", () -> hostGame(lobbyUri)),
            ShowPlayersAction.builder()
                .parentWindow(parent)
                .gameIdSelection(
                    () ->
                        gameTableModel.getGameListingForRow(
                            gameTable.convertRowIndexToModel(gameTable.getSelectedRow())))
                .playerToLobbyConnection(lobbyClient.getPlayerToLobbyConnection())
                .build()
                .buildSwingAction())
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
    return List.of(
        SwingAction.of("Show Chat History", e -> showChatHistory()),
        SwingAction.of("Boot Game", e -> bootGame()),
        SwingAction.of("Shutdown", e -> shutdown()));
  }

  private void joinGame() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }
    // we sort the table, so get the correct index
    final GameDescription description =
        gameTableModel.get(gameTable.convertRowIndexToModel(selectedIndex));
    GameProcess.joinGame(description, lobbyClient.getUserName());
  }

  private void hostGame(final URI lobbyUri) {
    final ServerOptions options =
        new ServerOptions(
            JOptionPane.getFrameForComponent(this), lobbyClient.getUserName(), 3300, true);
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
        lobbyUri);
  }

  private void showChatHistory() {
    final int selectedIndex = gameTable.getSelectedRow();
    if (selectedIndex == -1) {
      return;
    }

    FetchChatHistory.builder()
        .parentWindow(parent)
        .gameId(gameTableModel.getGameIdForRow(gameTable.convertRowIndexToModel(selectedIndex)))
        .gameHostName(gameTableModel.get(selectedIndex).getHostedBy().getName())
        .playerToLobbyConnection(lobbyClient.getPlayerToLobbyConnection())
        .build()
        .fetchAndShowChatHistory();
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
    lobbyClient.getPlayerToLobbyConnection().sendShutdownRequest(gameId);
    JOptionPane.showMessageDialog(null, "The game you selected was sent a shutdown signal");
  }
}
