package games.strategy.engine.lobby.client.ui;

import games.strategy.net.GUID;
import java.util.Map;
import java.util.Optional;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.triplea.lobby.common.GameDescription;
import org.triplea.lobby.common.ILobbyGameBroadcaster;

class LobbyGameTableModel extends AbstractTableModel implements ILobbyGameBroadcaster {
  private static final long serialVersionUID = 6399458368730633993L;

  enum Column {
    Host,
    Name,
    GV,
    Round,
    Players,
    P,
    Status,
    Comments,
    Started,
    GUID
  }

  private final GameListModel gameListModel;
  private final boolean isAdmin;

  LobbyGameTableModel(final Map<GUID, GameDescription> gameList, final boolean isAdmin) {
    this.isAdmin = isAdmin;
    gameListModel = new GameListModel();

    for (final Map.Entry<GUID, GameDescription> entry : gameList.entrySet()) {
      updateGame(entry.getKey(), entry.getValue());
    }
  }

  private void updateGame(final GUID gameId, final GameDescription description) {
    final Optional<Integer> rowIndex = gameListModel.updateOrAdd(gameId, description);
    SwingUtilities.invokeLater(
        () ->
            rowIndex.ifPresentOrElse(
                row -> fireTableRowsUpdated(row, row),
                () -> fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1)));
  }

  @Override
  public void gameUpdated(final GUID gameId, final GameDescription description) {
    if (gameId != null) {
      updateGame(gameId, description);
    }
  }

  @Override
  public void gameRemoved(final GUID gameId) {
    if (gameId == null) {
      return;
    }

    gameListModel
        .removeGame(gameId)
        .ifPresent(index -> SwingUtilities.invokeLater(() -> fireTableRowsDeleted(index, index)));
  }

  GameDescription get(final int i) {
    return gameListModel.getGameDescriptionByRow(i);
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
    final int adminHiddenColumns = isAdmin ? 0 : -1;
    // -1 so we don't display the guid
    // -1 again if we are not admin to hide the 'started' column
    return Column.values().length - 1 + adminHiddenColumns;
  }

  @Override
  public int getRowCount() {
    return gameListModel.size();
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final Column column = Column.values()[columnIndex];
    final GameDescription description = gameListModel.getGameDescriptionByRow(rowIndex);
    switch (column) {
      case Host:
        return description.getHostName();
      case Round:
        final int round = description.getRound();
        return round == 0 ? "-" : String.valueOf(round);
      case Name:
        return description.getGameName();
      case Players:
        return description.getPlayerCount();
      case P:
        return (description.isPassworded() ? "*" : "");
      case GV:
        return description.getGameVersion();
      case Status:
        return description.getStatus();
      case Comments:
        return description.getComment();
      case Started:
        return description.getFormattedBotStartTime();
      case GUID:
        return gameListModel.getGameGuidByRow(rowIndex);
      default:
        throw new IllegalStateException("Unknown column:" + column);
    }
  }
}
