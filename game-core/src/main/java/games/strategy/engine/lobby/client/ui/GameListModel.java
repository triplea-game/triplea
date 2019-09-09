package games.strategy.engine.lobby.client.ui;

import com.google.common.base.Preconditions;
import games.strategy.net.GUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.triplea.lobby.common.GameDescription;
import org.triplea.util.Tuple;

class GameListModel {
  private final List<Tuple<GUID, GameDescription>> gameList = new ArrayList<>();

  /**
   * Checks if a game identified by GUID is in the data model already or not.
   *
   * @return True if the table data model contains the given GUID, false otherwise.
   */
  boolean containsGame(final GUID gameId) {
    return findGame(gameId) != null;
  }

  /**
   * Adds a game identified by GUID.
   *
   * @param gameId GUID identifying the new game to insert.
   * @param description Description for the new game.
   */
  void add(final GUID gameId, final GameDescription description) {
    gameList.add(Tuple.of(gameId, description));
  }

  /**
   * Updates a game identified by GUID.
   *
   * @param gameId GUID identifying the game to update.
   * @param description The new description for the game.
   * @return Returns the row index of the updated game.
   */
  int update(final GUID gameId, final GameDescription description) {
    Preconditions.checkNotNull(gameId);
    final Tuple<GUID, GameDescription> toReplace = findGame(gameId);
    final int replaceIndex = gameList.indexOf(toReplace);
    gameList.set(replaceIndex, Tuple.of(gameId, description));
    return replaceIndex;
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
