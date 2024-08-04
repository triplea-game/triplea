package games.strategy.triplea.delegate.data;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import java.io.Serializable;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import lombok.Getter;

/**
 * Sent by the battle delegate to the game player to indicate which battles are left to be fought.
 */
@Getter
public class BattleListing implements Serializable {
  private static final long serialVersionUID = 2700129486225793827L;
  private final Map<BattleType, Collection<Territory>> battlesMap;

  /**
   * Creates new BattleListing.
   *
   * @param battles battles to list
   */
  public BattleListing(final Set<IBattle> battles) {
    this.battlesMap = new EnumMap<>(BattleType.class);
    battles.stream()
        .filter(b -> !b.isEmpty())
        .forEach(
            b -> {
              Collection<Territory> territories = battlesMap.get(b.getBattleType());
              if (territories == null) {
                territories = new HashSet<>();
              }
              territories.add(b.getTerritory());
              battlesMap.put(b.getBattleType(), territories);
            });
  }

  public Set<Territory> getNormalBattlesIncludingAirBattles() {
    return getBattlesWith(b -> !b.isBombingRun());
  }

  public Set<Territory> getStrategicBombingRaidsIncludingAirBattles() {
    return getBattlesWith(BattleType::isBombingRun);
  }

  private Set<Territory> getBattlesWith(Predicate<BattleType> predicate) {
    final Set<Territory> territories = new HashSet<>();
    for (final Entry<BattleType, Collection<Territory>> entry : battlesMap.entrySet()) {
      if (predicate.test(entry.getKey())) {
        territories.addAll(entry.getValue());
      }
    }
    return territories;
  }

  public boolean isEmpty() {
    return battlesMap.isEmpty();
  }

  public void forEachBattle(BiConsumer<? super BattleType, ? super Territory> action) {
    for (final Entry<BattleType, Collection<Territory>> battleTypeCollection :
        battlesMap.entrySet()) {
      final BattleType battleType = battleTypeCollection.getKey();
      for (final Territory territory : battleTypeCollection.getValue()) {
        action.accept(battleType, territory);
      }
    }
  }
}
