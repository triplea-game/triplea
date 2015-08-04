package games.puzzle.slidingtiles.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import games.puzzle.slidingtiles.attachments.Tile;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.player.GridAbstractAI;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridPlayData;

/**
 * AI agent for N-Puzzle.
 *
 * Plays by attempting to play on a random square on the board.
 *
 */
public class RandomAI extends GridAbstractAI {
  public RandomAI(final String name, final String type) {
    super(name, type);
  }

  @Override
  protected void play() {
    // Unless the triplea.ai.pause system property is set to false,
    // pause for 0.8 seconds to give the impression of thinking
    pause();
    // Get the collection of territories from the map
    final GameMap map = getGameData().getMap();
    final Collection<Territory> territories = map.getTerritories();
    // Get the play delegate
    final IGridPlayDelegate playDel = (IGridPlayDelegate) this.getPlayerBridge().getRemoteDelegate();
    final PlayerID me = getPlayerID();
    // Find the blank tile
    Territory blank = null;
    for (final Territory t : territories) {
      final Tile tile = (Tile) t.getAttachment("tile");
      if (tile != null) {
        final int value = tile.getValue();
        if (value == 0) {
          blank = t;
          break;
        }
      }
    }
    if (blank == null) {
      throw new RuntimeException("No blank tile");
    }
    final Random random = new Random();
    final List<Territory> neighbors = new ArrayList<Territory>(map.getNeighbors(blank));
    final Territory swap = neighbors.get(random.nextInt(neighbors.size()));
    final IGridPlayData play = new GridPlayData(swap, blank, me);
    playDel.play(play);
  }
}
