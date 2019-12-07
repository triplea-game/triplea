package games.strategy.engine.lobby.client.ui;

import com.google.common.annotations.VisibleForTesting;
import feign.FeignException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.triplea.http.client.lobby.HttpLobbyClient;
import org.triplea.http.client.lobby.game.listing.LobbyGame;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.java.timer.ScheduledTimer;
import org.triplea.java.timer.Timers;
import org.triplea.lobby.common.GameDescription;
import org.triplea.lobby.common.LobbyGameUpdateListener;
import org.triplea.util.Tuple;

class LobbyGameTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 6399458368730633993L;

  private static final int GAME_POLLER_FREQUENCY_SECONDS = 3;

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
    UUID
  }

  private final boolean admin;
  private final transient ScheduledTimer gamePoller;

  // these must only be accessed in the swing event thread
  private final List<Tuple<String, GameDescription>> gameList = new CopyOnWriteArrayList<>();
  private final LobbyGameUpdateListener lobbyGameBroadcaster =
      new LobbyGameUpdateListener() {
        @Override
        public void gameUpdated(final LobbyGameListing lobbyGameListing) {
          updateGame(
              lobbyGameListing.getGameId(),
              GameDescription.fromLobbyGame(lobbyGameListing.getLobbyGame()));
        }

        @Override
        public void gameRemoved(final String gameId) {
          removeGame(gameId);
        }
      };

  LobbyGameTableModel(
      final boolean admin,
      final HttpLobbyClient httpLobbyClient,
      final Consumer<String> errorMessageReporter) {
    this.admin = admin;

    // Poll period is chosen to be somewhat frequent so that there is not too much of noticeable
    // delay when games are updated, yet still as infrequent as possible to keep server load to
    // a minimum.
    gamePoller =
        Timers.fixedRateTimer("game-poller")
            .period(GAME_POLLER_FREQUENCY_SECONDS, TimeUnit.SECONDS)
            .delay(GAME_POLLER_FREQUENCY_SECONDS, TimeUnit.SECONDS)
            .task(
                new GamePollerTask(
                    lobbyGameBroadcaster,
                    gameListingSupplier(),
                    httpLobbyClient.getGameListingClient()::fetchGameListing,
                    errorMessageReporter))
            .start();
    httpLobbyClient.addConnectionClosedListener(msg -> gamePoller.cancel());

    try {
      final Map<String, GameDescription> games =
          httpLobbyClient.getGameListingClient().fetchGameListing().stream()
              .collect(
                  Collectors.toMap(LobbyGameListing::getGameId, GameDescription::fromLobbyGame));

      for (final Map.Entry<String, GameDescription> entry : games.entrySet()) {
        updateGame(entry.getKey(), entry.getValue());
      }
    } catch (final FeignException e) {
      gamePoller.cancel();
      throw new CouldNotConnectToLobby(e);
    }
  }

  private Supplier<Map<String, LobbyGame>> gameListingSupplier() {
    return () ->
        gameList.stream()
            .collect(Collectors.toMap(Tuple::getFirst, t -> t.getSecond().toLobbyGame()));
  }

  private static class CouldNotConnectToLobby extends RuntimeException {
    private static final long serialVersionUID = -651924799081225628L;

    CouldNotConnectToLobby(final FeignException e) {
      // TODO: Project#12 add up-time-robot link and/or link to report this error.
      super(e.getMessage(), e);
    }
  }

  private void removeGame(final String gameId) {
    SwingUtilities.invokeLater(
        () -> {
          if (gameId == null) {
            return;
          }

          final Tuple<String, GameDescription> gameToRemove = findGame(gameId);
          if (gameToRemove != null) {
            final int index = gameList.indexOf(gameToRemove);
            gameList.remove(gameToRemove);
            fireTableRowsDeleted(index, index);
          }
        });
  }

  private Tuple<String, GameDescription> findGame(final String gameId) {
    return gameList.stream()
        .filter(game -> game.getFirst().equals(gameId))
        .findFirst()
        .orElse(null);
  }

  @VisibleForTesting
  LobbyGameUpdateListener getLobbyGameBroadcaster() {
    return lobbyGameBroadcaster;
  }

  GameDescription get(final int i) {
    return gameList.get(i).getSecond();
  }

  String getGameIdForRow(final int i) {
    return gameList.get(i).getFirst();
  }

  private void updateGame(final String gameId, final GameDescription description) {
    SwingUtilities.invokeLater(
        () -> {
          final Tuple<String, GameDescription> toReplace = findGame(gameId);
          if (toReplace == null) {
            gameList.add(Tuple.of(gameId, description));
            fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
          } else {
            final int replaceIndex = gameList.indexOf(toReplace);
            gameList.set(replaceIndex, Tuple.of(gameId, description));
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
        return formatBotStartTime(description.getStartDateTime());
      case UUID:
        return gameList.get(rowIndex).getFirst();
      default:
        throw new IllegalStateException("Unknown column:" + column);
    }
  }

  @VisibleForTesting
  static String formatBotStartTime(final Instant instant) {
    return new DateTimeFormatterBuilder()
        .appendLocalized(null, FormatStyle.SHORT)
        .toFormatter()
        .format(LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
  }

  public void shutdown() {
    gamePoller.cancel();
  }
}
