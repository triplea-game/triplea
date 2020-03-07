package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.stats.AbstractStat;
import games.strategy.triplea.util.PlayerOrderComparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;

/** Hold some common things like GameData for stats panels and other tab panels. */
public abstract class AbstractStatPanel extends JPanel {
  private static final long serialVersionUID = 1906611524937548809L;
  protected GameData gameData;

  AbstractStatPanel(final GameData data) {
    gameData = data;
  }

  /** Returns all the alliances with more than one player. */
  public Collection<String> getAlliances() {
    return getAllianceMap().keySet();
  }

  Map<String, Set<GamePlayer>> getAllianceMap() {
    final Map<String, Set<GamePlayer>> allianceMap = new LinkedHashMap<>();
    for (final String alliance : gameData.getAllianceTracker().getAlliances()) {
      if (gameData.getAllianceTracker().getPlayersInAlliance(alliance).size() > 1) {
        allianceMap.put(alliance, gameData.getAllianceTracker().getPlayersInAlliance(alliance));
      }
    }
    return allianceMap;
  }

  public List<GamePlayer> getPlayers() {
    final List<GamePlayer> players = new ArrayList<>(gameData.getPlayerList().getPlayers());
    players.sort(new PlayerOrderComparator(gameData));
    return players;
  }

  public static class ResourceStat extends AbstractStat {
    final Resource resource;

    public ResourceStat(final Resource resource) {
      this.resource = resource;
    }

    @Override
    public String getName() {
      return resource == null ? "" : resource.getName();
    }

    @Override
    public double getValue(final GamePlayer player, final GameData data) {
      return player.getResources().getQuantity(resource);
    }
  }
}
