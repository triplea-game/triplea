package games.strategy.triplea.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import javax.swing.JPanel;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.stats.AbstractStat;
import games.strategy.triplea.util.PlayerOrderComparator;

/**
 * Hold some common things like GameData for stats panels and other tab panels.
 */
public abstract class AbstractStatPanel extends JPanel {
  private static final long serialVersionUID = 1906611524937548809L;
  protected GameData gameData;

  /**
   * Does not call initLayout() because initLayout may depend on some private tables being created with GameData first.
   * So make sure you call initLayout() yourself.
   */
  AbstractStatPanel(final GameData data) {
    gameData = data;
  }

  /**
   * You will need to call this yourself.
   */
  protected abstract void initLayout();

  public abstract void setGameData(final GameData data);

  /**
   * @return all the alliances with more than one player.
   */
  public Collection<String> getAlliances() {
    final Collection<String> alliances = new TreeSet<>();
    for (final String alliance : gameData.getAllianceTracker().getAlliances()) {
      if (gameData.getAllianceTracker().getPlayersInAlliance(alliance).size() > 1) {
        alliances.add(alliance);
      }
    }
    return alliances;
  }

  public List<PlayerID> getPlayers() {
    final List<PlayerID> players = new ArrayList<>(gameData.getPlayerList().getPlayers());
    players.sort(new PlayerOrderComparator(gameData));
    return players;
  }

  static class ResourceStat extends AbstractStat {
    final Resource resource;

    ResourceStat(final Resource resource) {
      super();
      this.resource = resource;
    }

    @Override
    public String getName() {
      return (resource == null) ? "" : resource.getName();
    }

    @Override
    public double getValue(final PlayerID player, final GameData data) {
      return player.getResources().getQuantity(resource);
    }
  }
}
