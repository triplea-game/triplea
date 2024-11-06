package games.strategy.engine.lobby.client.ui;

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.triplea.domain.data.LobbyGame;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.http.client.web.socket.messages.envelopes.game.listing.LobbyGameRemovedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.game.listing.LobbyGameUpdatedMessage;
import org.triplea.lobby.common.GameDescription;
import org.triplea.lobby.common.LobbyGameUpdateListener;

class LobbyGameTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 6399458368730633993L;

  enum Column {
    Host,
    Name,
    Round,
    Players,
    P,
    Status,
    Comments,
    Started,
    UUID
  }

  private final boolean admin;

  // these must only be accessed in the swing event thread
  private final List<LobbyGameListing> gameList = new CopyOnWriteArrayList<>();
  private final PlayerToLobbyConnection playerToLobbyConnection;
  private final LobbyGameUpdateListener lobbyGameBroadcaster =
      new LobbyGameUpdateListener() {
        @Override
        public void gameUpdated(final LobbyGameListing lobbyGameListing) {
          updateGame(lobbyGameListing);
        }

        @Override
        public void gameRemoved(final String gameId) {
          removeGame(gameId);
        }
      };

  LobbyGameTableModel(final boolean admin, final PlayerToLobbyConnection playerToLobbyConnection) {
    this.admin = admin;
    this.playerToLobbyConnection = playerToLobbyConnection;

    playerToLobbyConnection.addMessageListener(
        LobbyGameUpdatedMessage.TYPE,
        lobbyGameUpdatedMessage ->
            lobbyGameBroadcaster.gameUpdated(lobbyGameUpdatedMessage.getLobbyGameListing()));

    playerToLobbyConnection.addMessageListener(
        LobbyGameRemovedMessage.TYPE,
        lobbyGameRemovedMessage ->
            lobbyGameBroadcaster.gameRemoved(lobbyGameRemovedMessage.getGameId()));

    playerToLobbyConnection.fetchGameListing().forEach(lobbyGameBroadcaster::gameUpdated);
  }

  private void removeGame(final String gameId) {
    SwingUtilities.invokeLater(
        () -> {
          if (gameId == null) {
            return;
          }

          final LobbyGameListing gameToRemove = findGame(gameId);
          if (gameToRemove != null) {
            final int index = gameList.indexOf(gameToRemove);
            gameList.remove(gameToRemove);
            fireTableRowsDeleted(index, index);
          }
        });
  }

  private LobbyGameListing findGame(final String gameId) {
    return gameList.stream()
        .filter(game -> game.getGameId().equals(gameId))
        .findFirst()
        .orElse(null);
  }

  @VisibleForTesting
  LobbyGameUpdateListener getLobbyGameBroadcaster() {
    return lobbyGameBroadcaster;
  }

  GameDescription get(final int i) {
    return GameDescription.fromLobbyGame(gameList.get(i).getLobbyGame());
  }

  LobbyGameListing getGameListingForRow(final int i) {
    return gameList.get(i);
  }

  String getGameIdForRow(final int i) {
    return gameList.get(i).getGameId();
  }

  private void updateGame(final LobbyGameListing lobbyGameListing) {
    SwingUtilities.invokeLater(
        () -> {
          final LobbyGameListing toReplace = findGame(lobbyGameListing.getGameId());
          if (toReplace == null) {
            gameList.add(lobbyGameListing);
            fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
          } else {
            final int replaceIndex = gameList.indexOf(toReplace);
            gameList.set(replaceIndex, lobbyGameListing);
            fireTableRowsUpdated(replaceIndex, replaceIndex);
          }
        });
  }

  @Override
  public String getColumnName(final int column) {
    return Column.values()[column].toString();
  }

  int getColumnIndex(final Column column) {
    return column.ordinal();
  }

  @Override
  public int getColumnCount() {
    final int adminHiddenColumns = admin ? 0 : -1;
    // -1 so we don't display the UUID
    // -1 again if we are not admin to hide the 'started' column
    return Column.values().length - 1 + adminHiddenColumns;
  }

  @Override
  public int getRowCount() {
    return gameList.size();
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final Column column = Column.values()[columnIndex];
    final LobbyGame description = gameList.get(rowIndex).getLobbyGame();
    switch (column) {
      case Host:
        return description.getHostName();
      case Round:
        final int round = description.getGameRound();
        return round == 0 ? "-" : String.valueOf(round);
      case Name:
        return description.getMapName();
      case Players:
        return description.getPlayerCount();
      case P:
        return (description.getPassworded() ? "*" : "");
      case Status:
        // Note, we update status client side to avoid a headless game from reporting
        // a new status when players leave or join. We expect a player count of 0 in
        // headless games if there are no players connected.
        return description.getPlayerCount() == 0 ? "Available" : description.getStatus();
      case Comments:
        return description.getComments();
      case Started:
        return formatBotStartTime(description.getEpochMilliTimeStarted());
      case UUID:
        return gameList.get(rowIndex).getGameId();
      default:
        throw new IllegalStateException("Unknown column: " + column);
    }
  }

  @VisibleForTesting
  static String formatBotStartTime(final long milliEpoch) {
    final var instant = Instant.ofEpochMilli(milliEpoch);
    return new DateTimeFormatterBuilder()
        .appendLocalized(null, FormatStyle.SHORT)
        .toFormatter()
        .format(LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
  }

  public void shutdown() {
    playerToLobbyConnection.close();
  }

  public void bootGame(final int selectedIndex) {
    final String gameId = getGameIdForRow(selectedIndex);
    playerToLobbyConnection.bootGame(gameId);
  }
}
