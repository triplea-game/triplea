package games.strategy.triplea.delegate.battle.simulation;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleState;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Converts mutable engine battle state into a deterministic observation DTO. */
public final class BattleObservationFactory {
  private static final Comparator<UnitGroupKey> UNIT_GROUP_ORDER =
      Comparator.comparing(UnitGroupKey::owner)
          .thenComparing(UnitGroupKey::unitType)
          .thenComparingInt(UnitGroupKey::hits)
          .thenComparing(UnitGroupKey::alreadyMoved);

  private BattleObservationFactory() {}

  public static BattleObservation create(final BattleState battleState) {
    final BattleState.BattleStatus status = battleState.getStatus();
    return new BattleObservation(
        BattleObservation.CURRENT_SCHEMA_VERSION,
        battleState.getBattleId().toString(),
        battleState.getBattleSite().getName(),
        status.getRound(),
        status.getMaxRounds(),
        status.isOver(),
        status.isAmphibious(),
        status.isHeadless(),
        battleState.getPlayer(BattleState.Side.OFFENSE).getName(),
        battleState.getPlayer(BattleState.Side.DEFENSE).getName(),
        summarize(
            battleState.filterUnits(BattleState.UnitBattleFilter.ALIVE, BattleState.Side.OFFENSE)),
        summarize(
            battleState.filterUnits(BattleState.UnitBattleFilter.ALIVE, BattleState.Side.DEFENSE)),
        battleState.getAttackerRetreatTerritories().stream()
            .map(territory -> territory.getName())
            .sorted()
            .toList());
  }

  private static List<UnitGroupObservation> summarize(final Collection<Unit> units) {
    final Map<UnitGroupKey, Integer> counts = new TreeMap<>(UNIT_GROUP_ORDER);
    units.forEach(
        unit ->
            counts.merge(
                new UnitGroupKey(
                    unit.getOwner().getName(),
                    unit.getType().getName(),
                    unit.getHits(),
                    unit.getAlreadyMoved()),
                1,
                Integer::sum));
    return counts.entrySet().stream()
        .map(
            entry ->
                new UnitGroupObservation(
                    entry.getKey().owner(),
                    entry.getKey().unitType(),
                    entry.getKey().hits(),
                    entry.getKey().alreadyMoved(),
                    entry.getValue()))
        .toList();
  }

  private record UnitGroupKey(String owner, String unitType, int hits, BigDecimal alreadyMoved) {}
}
