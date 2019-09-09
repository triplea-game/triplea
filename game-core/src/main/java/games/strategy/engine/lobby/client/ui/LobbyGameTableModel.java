package games.strategy.engine.lobby.client.ui;

import games.strategy.net.GUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.triplea.lobby.common.GameDescription;
import org.triplea.lobby.common.ILobbyGameBroadcaster;
import org.triplea.util.Tuple;

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

  private final List<Tuple<GUID, GameDescription>> gameList = new ArrayList<>();

  private final boolean isAdmin;

  LobbyGameTableModel(final Map<GUID, GameDescription> gameList, final boolean isAdmin) {
    this.isAdmin = isAdmin;
    for (final Map.Entry<GUID, GameDescription> entry : gameList.entrySet()) {
      updateGame(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void gameUpdated(final GUID gameId, final GameDescription description) {
    if (gameId != null) {
      updateGame(gameId, description);
    }
  }

  @Override
  public void gameRemoved(final GUID gameId) {
    if (gameId != null) {
      removeGame(gameId);
    }
  }

  private void removeGame(final GUID gameId) {
    final Tuple<GUID, GameDescription> gameToRemove = findGame(gameId);
    if (gameToRemove != null) {
      final int index = gameList.indexOf(gameToRemove);
      SwingUtilities.invokeLater(
          () -> {
            gameList.remove(gameToRemove);
            fireTableRowsDeleted(index, index);
          });
    }
  }

  private Tuple<GUID, GameDescription> findGame(final GUID gameId) {
    return gameList.stream()
        .filter(game -> game.getFirst().equals(gameId))
        .findFirst()
        .orElse(null);
  }

  GameDescription get(final int i) {
    return gameList.get(i).getSecond();
  }

  private void updateGame(final GUID gameId, final GameDescription description) {
    final Tuple<GUID, GameDescription> toReplace = findGame(gameId);
    if (toReplace == null) {
      gameList.add(Tuple.of(gameId, description));
      SwingUtilities.invokeLater(() -> fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1));
    } else {
      final int replaceIndex = gameList.indexOf(toReplace);
      gameList.set(replaceIndex, Tuple.of(gameId, description));
      SwingUtilities.invokeLater(() -> fireTableRowsUpdated(replaceIndex, replaceIndex));
    }
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
    return gameList.size();
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final Column column = Column.values()[columnIndex];
    final GameDescription description = gameList.get(rowIndex).getSecond();
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
        return gameList.get(rowIndex).getFirst();
      default:
        throw new IllegalStateException("Unknown column:" + column);
    }
  }
}
