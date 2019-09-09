package games.strategy.engine.lobby.client.ui;

import games.strategy.net.GUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.triplea.lobby.common.GameDescription;
import org.triplea.util.Tuple;

class GameListModel {
  private final List<Tuple<GUID, GameDescription>> gameList = new ArrayList<>();

  /**
   * <p>Adds or updates a game identified by GUID.
   *
   * @param gameId GUID identifying a game to insert or if existing, to update.
   * @param description The new description for the game.
   * @return Returns the row index of the updated game.
   */
  Optional<Integer> updateOrAdd(final GUID gameId, final GameDescription description) {
    final Tuple<GUID, GameDescription> toReplace = findGame(gameId);
    if (toReplace == null) {
      gameList.add(Tuple.of(gameId, description));
      return Optional.empty();
    }
    final int replaceIndex = gameList.indexOf(toReplace);
    gameList.set(replaceIndex, Tuple.of(gameId, description));
    return Optional.of(replaceIndex);
  }

  private Tuple<GUID, GameDescription> findGame(final GUID gameId) {
    return gameList.stream()
        .filter(game -> game.getFirst().equals(gameId))
        .findFirst()
        .orElse(null);
  }

  Optional<Integer> removeGame(final GUID gameId) {
    final Tuple<GUID, GameDescription> gameToRemove = findGame(gameId);
    if (gameToRemove != null) {
      final int index = gameList.indexOf(gameToRemove);
      gameList.remove(gameToRemove);
      return Optional.of(index);
    }
    return Optional.empty();
  }

  int size() {
    return gameList.size();
  }

  GameDescription getGameDescriptionByRow(final int i) {
    return gameList.get(i).getSecond();
  }

  GUID getGameGuidByRow(final int rowIndex) {
    return gameList.get(rowIndex).getFirst();
  }
}
