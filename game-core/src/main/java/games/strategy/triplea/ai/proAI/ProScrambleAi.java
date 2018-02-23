package games.strategy.triplea.ai.proAI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/**
 * Pro scramble AI.
 */
class ProScrambleAi {

  private final ProOddsCalculator calc;

  ProScrambleAi(final ProAi ai) {
    calc = ai.getCalc();
  }

  HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo,
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
    final ProBattleResult minResult = calc.calculateBattleResults(scrambleTo, attackers, defenders, bombardingUnits);
    ProLogger
        .debug(scrambleTo + ", minTUVSwing=" + minResult.getTuvSwing() + ", minWin%=" + minResult.getWinPercentage());
    if ((minResult.getTuvSwing() <= 0) && (minResult.getWinPercentage() < (100 - ProData.minWinPercentage))) {
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
    final ProBattleResult maxResult = calc.calculateBattleResults(scrambleTo, attackers, defenders, bombardingUnits);
    ProLogger
        .debug(scrambleTo + ", maxTUVSwing=" + maxResult.getTuvSwing() + ", maxWin%=" + maxResult.getWinPercentage());
    if (maxResult.getTuvSwing() >= minResult.getTuvSwing()) {
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
        ProSortMoveOptionsUtils.sortUnitMoveOptions(unitDefendOptions);

    // Add one scramble unit at a time and check if final result is better than min result
    final List<Unit> unitsToScramble = new ArrayList<>();
    ProBattleResult result = minResult;
    for (final Unit u : sortedUnitDefendOptions.keySet()) {
      unitsToScramble.add(u);
      final List<Unit> currentDefenders = (List<Unit>) battle.getDefendingUnits();
      currentDefenders.addAll(unitsToScramble);
      result = calc.calculateBattleResults(scrambleTo, attackers, currentDefenders, bombardingUnits);
      ProLogger.debug(scrambleTo + ", TUVSwing=" + result.getTuvSwing() + ", Win%=" + result.getWinPercentage()
          + ", addedUnit=" + u);
      if ((result.getTuvSwing() <= 0) && (result.getWinPercentage() < (100 - ProData.minWinPercentage))) {
        break;
      }
    }
    if (result.getTuvSwing() >= minResult.getTuvSwing()) {
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
