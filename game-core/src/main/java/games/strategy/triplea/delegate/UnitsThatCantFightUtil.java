package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import org.triplea.java.PredicateBuilder;

/** Utility for detecting and removing units that can't land at the end of a phase. */
public class UnitsThatCantFightUtil {
  private final GameDataInjections gameData;

  public UnitsThatCantFightUtil(final GameDataInjections data) {
    gameData = data;
  }

  Collection<Territory> getTerritoriesWhereUnitsCantFight(final GamePlayer player) {
    final Predicate<Unit> enemyAttackUnits =
        Matches.enemyUnit(player, gameData).and(Matches.unitCanAttack(player));
    final Collection<Territory> cantFight = new ArrayList<>();
    for (final Territory current : gameData.getMap()) {
      final Predicate<Unit> ownedUnitsMatch =
          PredicateBuilder.of(Matches.unitIsInfrastructure().negate())
              .andIf(current.isWater(), Matches.unitIsLand().negate())
              .and(Matches.unitIsOwnedBy(player))
              .build();
      final int countAllOwnedUnits = current.getUnitCollection().countMatches(ownedUnitsMatch);
      final Collection<Unit> nonCombatUnits =
          current
              .getUnitCollection()
              .getMatches(ownedUnitsMatch.and(Matches.unitCanAttack(player).negate()));
      if (nonCombatUnits.isEmpty() || nonCombatUnits.size() != countAllOwnedUnits) {
        continue;
      }
      if (current.getUnitCollection().anyMatch(enemyAttackUnits)) {
        cantFight.add(current);
      }
    }
    return cantFight;
  }
}
