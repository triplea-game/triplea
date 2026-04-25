package games.strategy.engine.lobby.client.ui;

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;
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
  private final LobbyGameListingModel listingModel;

  LobbyGameTableModel(final boolean admin, final LobbyGameListingModel listingModel) {
    this.admin = admin;
    this.listingModel = listingModel;
    listingModel.addChangeListener(
        () -> SwingUtilities.invokeLater(this::fireTableDataChanged));
  }

  @VisibleForTesting
  LobbyGameUpdateListener getLobbyGameBroadcaster() {
    return listingModel.getLobbyGameBroadcaster();
  }

  GameDescription get(final int i) {
    return listingModel.getGameDescriptionForRow(i);
  }

  LobbyGameListing getGameListingForRow(final int i) {
    return listingModel.getGameListingForRow(i);
  }

  String getGameIdForRow(final int i) {
    return listingModel.getGameIdForRow(i);
  }

  int getColumnIndex(final Column column) {
    return column.ordinal();
  }

  @Override
  public String getColumnName(final int column) {
    return Column.values()[column].toString();
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
    return listingModel.getRowCount();
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final Column column = Column.values()[columnIndex];
    final var lobbyGame = listingModel.getGameListingForRow(rowIndex).getLobbyGame();
    return switch (column) {
      case Host -> lobbyGame.getHostName();
      case Round -> {
        final int round = lobbyGame.getGameRound();
        yield round == 0 ? "-" : String.valueOf(round);
      }
      case Name -> lobbyGame.getMapName();
      case Players -> lobbyGame.getPlayerCount();
      case P -> (lobbyGame.getPassworded() ? "*" : "");
      case Status ->
          // Note, we update status client side to avoid a headless game from reporting
          // a new status when players leave or join. We expect a player count of 0 in
          // headless games if there are no players connected.
          lobbyGame.getPlayerCount() == 0 ? "Available" : lobbyGame.getStatus();
      case Comments -> lobbyGame.getComments();
      case Started -> formatBotStartTime(lobbyGame.getEpochMilliTimeStarted());
      case UUID -> listingModel.getGameIdForRow(rowIndex);
      default -> throw new IllegalStateException("Unknown column: " + column);
    };
  }

  @VisibleForTesting
  static String formatBotStartTime(final long milliEpoch) {
    final var instant = Instant.ofEpochMilli(milliEpoch);
    return new DateTimeFormatterBuilder()
        .appendLocalized(null, FormatStyle.SHORT)
        .toFormatter()
        .format(LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
  }
}
