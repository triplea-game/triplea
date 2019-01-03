package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.util.PredicateBuilder;

/** Utility for detecting and removing units that can't land at the end of a phase. */
public class UnitsThatCantFightUtil {
  private final GameData gameData;

  public UnitsThatCantFightUtil(final GameData data) {
    gameData = data;
  }

  Collection<Territory> getTerritoriesWhereUnitsCantFight(final PlayerId player) {
    final Predicate<Unit> enemyAttackUnits =
        Matches.enemyUnit(player, gameData).and(Matches.unitCanAttack(player));
    final Collection<Territory> cantFight = new ArrayList<>();
    for (final Territory current : gameData.getMap()) {
      final Predicate<Unit> ownedUnitsMatch =
          PredicateBuilder.of(Matches.unitIsInfrastructure().negate())
              .andIf(current.isWater(), Matches.unitIsLand().negate())
              .and(Matches.unitIsOwnedBy(player))
              .build();
      final int countAllOwnedUnits = current.getUnits().countMatches(ownedUnitsMatch);
      final Collection<Unit> nonCombatUnits =
          current
              .getUnits()
              .getMatches(ownedUnitsMatch.and(Matches.unitCanAttack(player).negate()));
      if (nonCombatUnits.isEmpty() || nonCombatUnits.size() != countAllOwnedUnits) {
        continue;
      }
      if (current.getUnits().anyMatch(enemyAttackUnits)) {
        cantFight.add(current);
      }
    }
    return cantFight;
  }
}
