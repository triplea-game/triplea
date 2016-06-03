package games.strategy.triplea.ai.proAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.data.ProBattleResult;
import games.strategy.triplea.ai.proAI.logging.ProLogger;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.ai.proAI.util.ProOddsCalculator;
import games.strategy.triplea.ai.proAI.util.ProSortMoveOptionsUtils;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pro scramble AI.
 */
public class ProScrambleAI {

  private final ProOddsCalculator calc;

  public ProScrambleAI(final ProAI ai) {
    calc = ai.getCalc();
  }

  public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo,
      final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers) {

    // Get battle data
    final GameData data = ProData.getData();
    final PlayerID player = ProData.getPlayer();
    final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
    final IBattle battle = delegate.getBattleTracker().getPendingBattle(scrambleTo, false, BattleType.NORMAL);

    // Check if defense already wins
    final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
    final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
    final Set<Unit> bombardingUnits = new HashSet<>(battle.getBombardingUnits());
    final ProBattleResult minResult =
        calc.calculateBattleResults(player, scrambleTo, attackers, defenders, bombardingUnits, false);
    ProLogger.debug(scrambleTo + ", minTUVSwing=" + minResult.getTUVSwing() + ", minWin%="
        + minResult.getWinPercentage());
    if (minResult.getTUVSwing() <= 0 && minResult.getWinPercentage() < (100 - ProData.minWinPercentage)) {
      return null;
    }

    // Check if max defense is worse
    final Set<Unit> allScramblers = new HashSet<>();
    final Map<Territory, List<Unit>> possibleMaxScramblerMap = new HashMap<>();
    for (final Territory t : possibleScramblers.keySet()) {
      final int maxCanScramble = BattleDelegate.getMaxScrambleCount(possibleScramblers.get(t).getFirst());
      List<Unit> canScrambleAir = new ArrayList<>(possibleScramblers.get(t).getSecond());
      if (maxCanScramble < canScrambleAir.size()) {
        Collections.sort(canScrambleAir, (o1, o2) -> {
          final double strength1 =
              ProBattleUtils.estimateStrength(scrambleTo, Collections.singletonList(o1), new ArrayList<>(), false);
          final double strength2 =
              ProBattleUtils.estimateStrength(scrambleTo, Collections.singletonList(o2), new ArrayList<>(), false);
          return Double.compare(strength2, strength1);
        });
        canScrambleAir = canScrambleAir.subList(0, maxCanScramble);
      }
      allScramblers.addAll(canScrambleAir);
      possibleMaxScramblerMap.put(t, canScrambleAir);
    }
    defenders.addAll(allScramblers);
    final ProBattleResult maxResult =
        calc.calculateBattleResults(player, scrambleTo, attackers, defenders, bombardingUnits, false);
    ProLogger.debug(scrambleTo + ", maxTUVSwing=" + maxResult.getTUVSwing() + ", maxWin%="
        + maxResult.getWinPercentage());
    if (maxResult.getTUVSwing() >= minResult.getTUVSwing()) {
      return null;
    }

    // Loop through all units and determine attack options
    final Map<Unit, Set<Territory>> unitDefendOptions = new HashMap<>();
    for (final Territory t : possibleMaxScramblerMap.keySet()) {
      final Set<Territory> possibleTerritories =
          data.getMap().getNeighbors(t, ProMatches.territoryCanMoveSeaUnits(player, data, true));
      possibleTerritories.add(t);
      final Set<Territory> battleTerritories = new HashSet<>();
      for (final Territory possibleTerritory : possibleTerritories) {
        final IBattle possibleBattle =
            delegate.getBattleTracker().getPendingBattle(possibleTerritory, false, BattleType.NORMAL);
        if (possibleBattle != null) {
          battleTerritories.add(possibleTerritory);
        }
      }
      for (final Unit u : possibleMaxScramblerMap.get(t)) {
        unitDefendOptions.put(u, battleTerritories);
      }
    }

    // Sort units by number of defend options and cost
    final Map<Unit, Set<Territory>> sortedUnitDefendOptions =
        ProSortMoveOptionsUtils.sortUnitMoveOptions(player, unitDefendOptions);

    // Add one scramble unit at a time and check if final result is better than min result
    final List<Unit> unitsToScramble = new ArrayList<>();
    ProBattleResult result = minResult;
    for (final Unit u : sortedUnitDefendOptions.keySet()) {
      unitsToScramble.add(u);
      final List<Unit> currentDefenders = (List<Unit>) battle.getDefendingUnits();
      currentDefenders.addAll(unitsToScramble);
      result = calc.calculateBattleResults(player, scrambleTo, attackers, currentDefenders, bombardingUnits, false);
      ProLogger.debug(scrambleTo + ", TUVSwing=" + result.getTUVSwing() + ", Win%=" + result.getWinPercentage()
          + ", addedUnit=" + u);
      if (result.getTUVSwing() <= 0 && result.getWinPercentage() < (100 - ProData.minWinPercentage)) {
        break;
      }
    }
    if (result.getTUVSwing() >= minResult.getTUVSwing()) {
      return null;
    }

    // Return units to scramble
    final HashMap<Territory, Collection<Unit>> scrambleMap = new HashMap<>();
    for (final Territory t : possibleScramblers.keySet()) {
      for (final Unit u : possibleScramblers.get(t).getSecond()) {
        if (unitsToScramble.contains(u)) {
          if (scrambleMap.containsKey(t)) {
            scrambleMap.get(t).add(u);
          } else {
            final Collection<Unit> units = new ArrayList<>();
            units.add(u);
            scrambleMap.put(t, units);
          }
        }
      }
    }
    return scrambleMap;
  }
}
