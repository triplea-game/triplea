package games.strategy.triplea.delegate.dataObjects;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.IBattle.BattleType;

/**
 * Sent by the battle delegate to the game player to indicate
 * which battles are left to be fought.
 */
public class BattleListing implements Serializable {
  private static final long serialVersionUID = 2700129486225793827L;
  private final Map<BattleType, Collection<Territory>> m_battles;

  /**
   * Creates new BattleListingMessage
   *
   * @param battles
   *        battles to list
   * @param strategicRaids
   *        strategic raids
   */
  public BattleListing(final Map<BattleType, Collection<Territory>> battles) {
    m_battles = battles;
  }

  public Map<BattleType, Collection<Territory>> getBattles() {
    return m_battles;
  }

  public Set<Territory> getAllBattleTerritories() {
    final Set<Territory> territories = new HashSet<>();
    for (final Entry<BattleType, Collection<Territory>> entry : m_battles.entrySet()) {
      territories.addAll(entry.getValue());
    }
    return territories;
  }

  public Set<Territory> getNormalBattlesIncludingAirBattles() {
    final Set<Territory> territories = new HashSet<>();
    for (final Entry<BattleType, Collection<Territory>> entry : m_battles.entrySet()) {
      if (!entry.getKey().isBombingRun()) {
        territories.addAll(entry.getValue());
      }
    }
    return territories;
  }

  public Set<Territory> getStrategicBombingRaidsIncludingAirBattles() {
    final Set<Territory> territories = new HashSet<>();
    for (final Entry<BattleType, Collection<Territory>> entry : m_battles.entrySet()) {
      if (entry.getKey().isBombingRun()) {
        territories.addAll(entry.getValue());
      }
    }
    return territories;
  }

  public Set<Territory> getAirBattles() {
    final Set<Territory> territories = new HashSet<>();
    for (final Entry<BattleType, Collection<Territory>> entry : m_battles.entrySet()) {
      if (entry.getKey().isAirPreBattleOrPreRaid()) {
        territories.addAll(entry.getValue());
      }
    }
    return territories;
  }

  public boolean isEmpty() {
    return m_battles.isEmpty();
  }
}
