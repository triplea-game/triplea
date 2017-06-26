package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.util.Match;

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
    final Match<Unit> enemyAttackUnits = Match.all(Matches.enemyUnit(player, m_data), Matches.unitCanAttack(player));
    final Collection<Territory> cantFight = new ArrayList<>();
    for (final Territory current : m_data.getMap()) {
      // get all owned non-combat units
      final Match.CompositeBuilder<Unit> ownedUnitsMatchBuilder = Match.newCompositeBuilder(
          Matches.UnitIsInfrastructure.invert());
      if (current.isWater()) {
        ownedUnitsMatchBuilder.add(Matches.UnitIsLand.invert());
      }
      ownedUnitsMatchBuilder.add(Matches.unitIsOwnedBy(player));
      // All owned units
      final int countAllOwnedUnits = current.getUnits().countMatches(ownedUnitsMatchBuilder.all());
      // only noncombat units
      ownedUnitsMatchBuilder.add(Matches.unitCanAttack(player).invert());
      final Collection<Unit> nonCombatUnits = current.getUnits().getMatches(ownedUnitsMatchBuilder.all());
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
