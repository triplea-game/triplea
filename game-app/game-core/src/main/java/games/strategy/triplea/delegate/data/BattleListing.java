package games.strategy.triplea.delegate.data;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.Getter;

/**
 * Sent by the battle delegate to the game player to indicate which battles are left to be fought.
 */
@Getter
public class BattleListing implements Serializable {
  private static final long serialVersionUID = 2700129486225793827L;
  private final Map<BattleType, Collection<Territory>> battles;

  /**
   * Creates new BattleListing.
   *
   * @param battles battles to list
   */
  public BattleListing(final Map<BattleType, Collection<Territory>> battles) {
    this.battles = battles;
  }

  public Set<Territory> getNormalBattlesIncludingAirBattles() {
    final Set<Territory> territories = new HashSet<>();
    for (final Entry<BattleType, Collection<Territory>> entry : battles.entrySet()) {
      if (!entry.getKey().isBombingRun()) {
        territories.addAll(entry.getValue());
      }
    }
    return territories;
  }

  public Set<Territory> getStrategicBombingRaidsIncludingAirBattles() {
    final Set<Territory> territories = new HashSet<>();
    for (final Entry<BattleType, Collection<Territory>> entry : battles.entrySet()) {
      if (entry.getKey().isBombingRun()) {
        territories.addAll(entry.getValue());
      }
    }
    return territories;
  }

  public boolean isEmpty() {
    return battles.isEmpty();
  }

  public void forEachBattle(BiConsumer<? super BattleType, ? super Territory> action) {
    for (final Entry<BattleType, Collection<Territory>> battleTypeCollection : battles.entrySet()) {
      final BattleType battleType = battleTypeCollection.getKey();
      for (final Territory territory : battleTypeCollection.getValue()) {
        action.accept(battleType, territory);
      }
    }
  }
}
