package games.strategy.engine.lobby.client.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.lobby.server.ILobbyGameBroadcaster;
import games.strategy.engine.lobby.server.ILobbyGameController;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.GUID;
import games.strategy.net.IMessenger;
import games.strategy.util.Tuple;

public class LobbyGameTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 6399458368730633993L;

  enum Column {
    Host, Name, GV, Round, Players, P, B, EV, Started, Status, Comments, GUID
  }

  private final IMessenger m_messenger;
  private final IChannelMessenger m_channelMessenger;
  private final IRemoteMessenger m_remoteMessenger;

  // these must only be accessed in the swing event thread
  private final List<Tuple<GUID,GameDescription>> gameList;
  private final ILobbyGameBroadcaster lobbyGameBroadcaster;

  public LobbyGameTableModel(final IMessenger messenger, final IChannelMessenger channelMessenger,
      final IRemoteMessenger remoteMessenger) {

    gameList = new ArrayList<Tuple<GUID,GameDescription>>();

    m_messenger = messenger;
    m_channelMessenger = channelMessenger;
    m_remoteMessenger = remoteMessenger;
    lobbyGameBroadcaster = new ILobbyGameBroadcaster() {
      @Override
      public void gameUpdated(final GUID gameId, final GameDescription description) {
        assertSentFromServer();
        updateGame(gameId, description);
      }
      
      /**
       * @deprecated Call gameUpdated instead, it will add or update
       */
      @Override
      public void gameAdded(final GUID gameId, final GameDescription description) {
        assertSentFromServer();
        updateGame(gameId, description);
      }

      @Override
      public void gameRemoved(final GUID gameId) {
        assertSentFromServer();
        removeGame(gameId);
      }
    };
    m_channelMessenger.registerChannelSubscriber(lobbyGameBroadcaster, ILobbyGameBroadcaster.GAME_BROADCASTER_CHANNEL);

    final Map<GUID, GameDescription> games =
        ((ILobbyGameController) m_remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE)).listGames();
    for (final GUID id : games.keySet()) {
      updateGame(id, games.get(id));
    }
  }

  private void removeGame(final GUID gameId) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (gameId == null) {
          return;
        }

        final Tuple<GUID, GameDescription> gameToRemove = findGame(gameId);
        if (gameToRemove != null) {
          final int index = gameList.indexOf(gameToRemove);
          gameList.remove(gameToRemove);
          fireTableRowsDeleted(index, index);
        }
      }
    });
  }

  private Tuple<GUID, GameDescription> findGame(final GUID gameId) {
    for (final Tuple<GUID, GameDescription> game : gameList) {
      if (game.getFirst().equals(gameId)) {
        return game;
      }
    }
    return null;
  }


  protected ILobbyGameBroadcaster getLobbyGameBroadcaster() {
    return lobbyGameBroadcaster;
  }

  public GameDescription get(final int i) {
    return gameList.get(i).getSecond();
  }

  @Override
  public Class<?> getColumnClass(final int columnIndex) {
    if (columnIndex == getColumnIndex(Column.Started)) {
      return Date.class;
    }
    return Object.class;
  }

  private void assertSentFromServer() {
    if (!MessageContext.getSender().equals(m_messenger.getServerNode())) {
      throw new IllegalStateException("Invalid sender");
    }
  }

  private void updateGame(final GUID gameId, final GameDescription description) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (gameId == null) {
          return;
        }

        final Tuple<GUID, GameDescription> toReplace = findGame(gameId);
        if (toReplace == null) {
          gameList.add(Tuple.of(gameId, description));
        } else {
          final int replaceIndex = gameList.indexOf(toReplace);
          gameList.set(replaceIndex, Tuple.of(gameId, description));
          fireTableRowsUpdated(replaceIndex, replaceIndex);
        }
      }
    });
  }

  @Override
  public String getColumnName(final int column) {
    return Column.values()[column].toString();
  }

  public int getColumnIndex(final Column column) {
    return column.ordinal();
  }

  @Override
  public int getColumnCount() {
    // -1 so we don't display the guid
    return Column.values().length - 1;
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
        return description.getRound();
      case Name:
        return description.getGameName();
      case Players:
        return description.getPlayerCount();
      case P:
        return (description.getPassworded() ? "*" : "");
      case B:
        return (description.getBotSupportEmail() != null && description.getBotSupportEmail().length() > 0 ? "-" : "");
      case GV:
        return description.getGameVersion();
      case EV:
        return description.getEngineVersion();
      case Status:
        return description.getStatus();
      case Comments:
        return description.getComment();
      case Started:
        return description.getStartDateTime();
      case GUID:
        return gameList.get(rowIndex).getFirst();
      default:
        throw new IllegalStateException("Unknown column:" + column);
    }
  }
}
