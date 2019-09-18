package games.strategy.engine.lobby.client.ui;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.triplea.lobby.common.GameDescription;
import org.triplea.util.Tuple;

class GameListModel {
  private final List<Tuple<UUID, GameDescription>> gameList = new ArrayList<>();

  /**
   * Checks if a game identified by UUID is in the data model already or not.
   *
   * @return True if the table data model contains the given UUID, false otherwise.
   */
  boolean containsGame(final UUID gameId) {
    return findGame(gameId) != null;
  }

  /**
   * Adds a game identified by UUID.
   *
   * @param gameId UUID identifying the new game to insert.
   * @param description Description for the new game.
   */
  void add(final UUID gameId, final GameDescription description) {
    gameList.add(Tuple.of(gameId, description));
  }

  /**
   * Updates a game identified by UUID.
   *
   * @param gameId UUID identifying the game to update.
   * @param description The new description for the game.
   * @return Returns the row index of the updated game.
   */
  int update(final UUID gameId, final GameDescription description) {
    Preconditions.checkNotNull(gameId);
    final Tuple<UUID, GameDescription> toReplace = findGame(gameId);
    final int replaceIndex = gameList.indexOf(toReplace);
    gameList.set(replaceIndex, Tuple.of(gameId, description));
    return replaceIndex;
  }

  private Tuple<UUID, GameDescription> findGame(final UUID gameId) {
    return gameList.stream()
        .filter(game -> game.getFirst().equals(gameId))
        .findFirst()
        .orElse(null);
  }

  Optional<Integer> removeGame(final UUID gameId) {
    final Tuple<UUID, GameDescription> gameToRemove = findGame(gameId);
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

  UUID getGameGuidByRow(final int rowIndex) {
    return gameList.get(rowIndex).getFirst();
  }
}
