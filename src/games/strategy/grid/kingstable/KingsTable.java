package games.strategy.grid.kingstable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.grid.GridGame;
import games.strategy.grid.kingstable.player.BetterAI;
import games.strategy.grid.kingstable.ui.KingsTableMapPanel;
import games.strategy.grid.kingstable.ui.KingsTableMenu;
import games.strategy.grid.player.GridGamePlayer;
import games.strategy.grid.player.RandomAI;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridGameMenu;
import games.strategy.grid.ui.GridMapData;
import games.strategy.grid.ui.GridMapPanel;

/**
 * Main class responsible for a Kings Table game.
 */
public class KingsTable extends GridGame implements IGameLoader {
  private static final long serialVersionUID = -4160546942612903442L;
  private static final String HUMAN_PLAYER_TYPE = "Human";
  private static final String RANDOM_COMPUTER_PLAYER_TYPE = "Random AI";
  private static final String ALPHABETA_COMPUTER_PLAYER_TYPE = "\u03B1\u03B2 AI";// "αβ AI";

  // Minimax technically "works" for King's Table,
  // but the branching factor is so high that Minimax will run out of memory before it returns
  // private static final String MINIMAX_COMPUTER_PLAYER_TYPE = "Minimax AI";
  @Override
  public Set<IGamePlayer> createPlayers(final Map<String, String> playerNames) {
    final Set<IGamePlayer> players = new HashSet<IGamePlayer>();
    final Iterator<String> iter = playerNames.keySet().iterator();
    while (iter.hasNext()) {
      final String name = iter.next();
      final String type = playerNames.get(name);
      if (type.equals(HUMAN_PLAYER_TYPE) || type.equals(CLIENT_PLAYER_TYPE)) {
        final GridGamePlayer player = new GridGamePlayer(name, type);
        players.add(player);
      } else if (type.equals(RANDOM_COMPUTER_PLAYER_TYPE)) {
        final RandomAI ai = new RandomAI(name, type);
        players.add(ai);
      }
      // else if (type.equals(MINIMAX_COMPUTER_PLAYER_TYPE))
      // {
      // BetterAI ai = new BetterAI(name, BetterAI.Algorithm.MINIMAX);
      // players.add(ai);
      // }
      else if (type.equals(ALPHABETA_COMPUTER_PLAYER_TYPE)) {
        final BetterAI ai = new BetterAI(name, type, BetterAI.Algorithm.ALPHABETA);
        players.add(ai);
      } else {
        throw new IllegalStateException("Player type not recognized:" + type);
      }
    }
    return players;
  }

  /**
   * Return an array of player types that can play on the server.
   */
  @Override
  public String[] getServerPlayerTypes() {
    return new String[] {HUMAN_PLAYER_TYPE, ALPHABETA_COMPUTER_PLAYER_TYPE, RANDOM_COMPUTER_PLAYER_TYPE};
    // {HUMAN_PLAYER_TYPE, ALPHABETA_COMPUTER_PLAYER_TYPE, MINIMAX_COMPUTER_PLAYER_TYPE, RANDOM_COMPUTER_PLAYER_TYPE};
  }

  @Override
  protected Class<? extends GridMapPanel> getGridMapPanelClass() {
    return KingsTableMapPanel.class;
  }

  @Override
  protected Class<? extends GridMapData> getGridMapDataClass() {
    return GridMapData.class;
  }

  @Override
  protected Class<? extends GridGameMenu<GridGameFrame>> getGridTableMenuClass() {
    return KingsTableMenu.class;
  }
}
