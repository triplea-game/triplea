package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;

/**
 * Utility for detecting and removing units that can't land at the end of a phase.
 */
public class UnitsThatCantFightUtil {
  private final GameData m_data;

  public UnitsThatCantFightUtil(final GameData data) {
    m_data = data;
  }

  // TODO Used to notify of kamikazi attacks
  Collection<Territory> getTerritoriesWhereUnitsCantFight(final PlayerID player) {
    final CompositeMatch<Unit> enemyAttackUnits = new CompositeMatchAnd<>();
    enemyAttackUnits.add(Matches.enemyUnit(player, m_data));
    enemyAttackUnits.add(Matches.unitCanAttack(player));
    final Collection<Territory> cantFight = new ArrayList<>();
    for (final Territory current : m_data.getMap()) {
      // get all owned non-combat units
      final CompositeMatch<Unit> ownedUnitsMatch = new CompositeMatchAnd<>();
      ownedUnitsMatch.add(Matches.UnitIsInfrastructure.invert());
      if (current.isWater()) {
        ownedUnitsMatch.add(Matches.UnitIsLand.invert());
      }
      ownedUnitsMatch.add(Matches.unitIsOwnedBy(player));
      // All owned units
      final int countAllOwnedUnits = current.getUnits().countMatches(ownedUnitsMatch);
      // only noncombat units
      ownedUnitsMatch.add(Matches.unitCanAttack(player).invert());
      final Collection<Unit> nonCombatUnits = current.getUnits().getMatches(ownedUnitsMatch);
      if (nonCombatUnits.isEmpty() || nonCombatUnits.size() != countAllOwnedUnits) {
        continue;
      }
      if (current.getUnits().someMatch(enemyAttackUnits)) {
        cantFight.add(current);
      }
    }
    return cantFight;
  }
}
