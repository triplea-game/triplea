package games.strategy.triplea.ai.proAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.net.GUID;
import games.strategy.triplea.ai.proAI.util.ProLogUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

/**
 * Pro retreat AI.
 * <ol>
 * <li>Consider whether submerging increases/decreases TUV swing</li>
 * <li>Consider what territory needs units when retreating</li>
 * </ol>
 * AFAIK there are 2 options available for maps (land battles):
 * 1. air can retreat separately on an amphib attack
 * 2. non-amphib land can retreat separately
 * So the result would be 4 situations:
 * 1. revised: you can't retreat anything on amphib
 * 2. only air can retreat on amphib
 * 3. only non-amphib land can retreat on amphib
 * 4. aa50: air and non-amphib land can retreat on amphib
 * Check by following TripleA.Constants -> TripleA.Properties statis get methods -> MustFightBattle
 * For sea battles you can have:
 * 1. attacker retreats all units at end of battle
 * 2. attacker submerges sub at start or end of battle
 * 3. defender submerges (or moves if Classic rules) sub at start or end of battle
 */
public class ProRetreatAI {

  private final ProAI ai;
  private final ProBattleUtils battleUtils;

  public ProRetreatAI(final ProAI ai, final ProBattleUtils battleUtils) {
    this.ai = ai;
    this.battleUtils = battleUtils;
  }

  public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleTerritory,
      final Collection<Territory> possibleTerritories, final String message) {

    // Get battle data
    final GameData data = ai.getGameData();
    final PlayerID player = ai.getPlayerID();
    final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
    final IBattle battle = delegate.getBattleTracker().getPendingBattle(battleID);

    // Get units and determine if attacker
    final boolean isAttacker = player.equals(battle.getAttacker());
    final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
    final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();

    // Calculate battle results
    final ProBattleResultData result =
        battleUtils.calculateBattleResults(player, battleTerritory, attackers, defenders, new HashSet<Unit>(),
            isAttacker);

    // Determine if it has a factory
    int isFactory = 0;
    if (ProMatches.territoryHasInfraFactoryAndIsLand(player).match(battleTerritory)) {
      isFactory = 1;
    }

    // Determine production value and if it is a capital
    int production = 0;
    int isCapital = 0;
    final TerritoryAttachment ta = TerritoryAttachment.get(battleTerritory);
    if (ta != null) {
      production = ta.getProduction();
      if (ta.isCapital()) {
        isCapital = 1;
      }
    }

    // Calculate current attack value
    double territoryValue = 0;
    if (result.isHasLandUnitRemaining() || Match.noneMatch(attackers, Matches.UnitIsAir)) {
      territoryValue = result.getWinPercentage() / 100 * (2 * production * (1 + isFactory) * (1 + isCapital));
    }
    double battleValue = result.getTUVSwing() + territoryValue;
    if (!isAttacker) {
      battleValue = -battleValue;
    }

    // Decide if we should retreat
    if (battleValue < 0) {

      // Retreat to capital if available otherwise the territory with highest defense strength
      Territory retreatTerritory = null;
      double maxStrength = Double.NEGATIVE_INFINITY;
      final Territory myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
      for (final Territory t : possibleTerritories) {
        if (t.equals(myCapital)) {
          retreatTerritory = t;
          break;
        }
        final double strength =
            battleUtils.estimateStrength(player, t, t.getUnits().getMatches(Matches.isUnitAllied(player, data)),
                new ArrayList<Unit>(), false);
        if (strength > maxStrength) {
          retreatTerritory = t;
          maxStrength = strength;
        }
      }
      ProLogUtils.log(Level.FINER, player.getName() + " retreating from territory " + battleTerritory + " to "
          + retreatTerritory + " because AttackValue=" + battleValue + ", TUVSwing=" + result.getTUVSwing()
          + ", possibleTerritories=" + possibleTerritories.size());
      return retreatTerritory;
    }
    ProLogUtils.log(Level.FINER, player.getName() + " not retreating from territory " + battleTerritory
        + " with AttackValue=" + battleValue + ", TUVSwing=" + result.getTUVSwing());

    return null;
  }
}
