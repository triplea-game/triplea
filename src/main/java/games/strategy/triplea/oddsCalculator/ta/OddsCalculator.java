package games.strategy.triplea.oddsCalculator.ta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.BattleTracker;
import games.strategy.triplea.delegate.GameDelegateBridge;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MustFightBattle;
import games.strategy.triplea.oddscalc.DummyDelegateBridge;
import games.strategy.triplea.oddscalc.OddsCalculatorParameters;
import games.strategy.util.Tuple;

/**
 * Simulates battles and reports results of winner/defender win/lose rate.
 */
public class OddsCalculator implements IOddsCalculator {
  public static final String OOL_ALL = "*";
  public static final String OOL_SEPARATOR = ";";
  private static final String OOL_SEPARATOR_REGEX = ";";
  public static final String OOL_AMOUNT_DESCRIPTOR = "^";
  private static final String OOL_AMOUNT_DESCRIPTOR_REGEX = "\\^";
  private boolean cancelled = false;


  @Override
  public AggregateResults calculate(final OddsCalculatorParameters parameters) {
    final long start = System.currentTimeMillis();
    final AggregateResults aggregateResults = new AggregateResults(parameters.runCount);

    for (int i = 0; i < parameters.runCount && !cancelled; i++) {
      doSimulation(parameters);
      aggregateResults.addResult(doSimulation(parameters));
    }
    aggregateResults.setTime(System.currentTimeMillis() - start);
    return aggregateResults;
  }

  static BattleResults doSimulation(final OddsCalculatorParameters parameters) {
    final List<Unit> attackerOrderOfLosses = OddsCalculator.getUnitListByOrderOfLoss(
        parameters.attackerOrderOfLosses,
        parameters.attacking,
        parameters.gameData);

    final List<Unit> defenderOrderOfLosses = OddsCalculator.getUnitListByOrderOfLoss(
        parameters.defenderOrderOfLosses,
        parameters.defending,
        parameters.gameData);

    final CompositeChange allChanges = new CompositeChange();
    final DummyDelegateBridge bridge1 =
        new DummyDelegateBridge(
            parameters.attacker,
            parameters.gameData,
            allChanges,
            attackerOrderOfLosses,
            defenderOrderOfLosses,
            parameters.keepOneAttackingLandUnit,
            parameters.retreatAfterRound,
            parameters.retreatAfterXUnitsLeft,
            parameters.retreatWhenOnlyAirLeft);
    final IDelegateBridge bridge = new GameDelegateBridge(bridge1);
    final MustFightBattle battle = new MustFightBattle(
        parameters.location,
        parameters.attacker,
        parameters.gameData,
        new BattleTracker());
    battle.setHeadless(true);
    battle.setUnits(
        parameters.defending,
        parameters.attacking,
        parameters.bombarding,
        (parameters.amphibious ? parameters.attacking : new ArrayList<>()), parameters.defender,
        parameters.territoryEffects);
    bridge1.setBattle(battle);
    battle.fight(bridge);
    return new BattleResults(battle, parameters.gameData);
  }

  @Override
  public void cancel() {
    cancelled = true;
  }

  /**
   * Validates if an order of loss string is good or not.
   * TODO: refactor this method... consider a better javaodc that tell us what a valid OOL string would be.
   * @return true if orderOfLoss string looks good.
   */
  public static boolean isValidOrderOfLoss(final String orderOfLoss, final GameData data) {
    if (orderOfLoss == null || orderOfLoss.trim().length() == 0) {
      return true;
    }
    try {
      final String[] sections;
      if (orderOfLoss.contains(OOL_SEPARATOR)) {
        sections = orderOfLoss.trim().split(OOL_SEPARATOR_REGEX);
      } else {
        sections = new String[1];
        sections[0] = orderOfLoss.trim();
      }
      final UnitTypeList unitTypes;
      try {
        data.acquireReadLock();
        unitTypes = data.getUnitTypeList();
      } finally {
        data.releaseReadLock();
      }
      for (final String section : sections) {
        if (section.length() == 0) {
          continue;
        }
        final String[] amountThenType = section.split(OOL_AMOUNT_DESCRIPTOR_REGEX);
        if (amountThenType.length != 2) {
          return false;
        }
        if (!amountThenType[0].equals(OOL_ALL)) {
          final int amount = Integer.parseInt(amountThenType[0]);
          if (amount <= 0) {
            return false;
          }
        }
        final UnitType type = unitTypes.getUnitType(amountThenType[1]);
        if (type == null) {
          return false;
        }
      }
    } catch (final Exception e) {
      return false;
    }
    return true;
  }

  private static List<Unit> getUnitListByOrderOfLoss(final String ool, final Collection<Unit> units,
      final GameData data) {
    if (ool == null || ool.trim().length() == 0) {
      return null;
    }
    final List<Tuple<Integer, UnitType>> map = new ArrayList<>();
    final String[] sections;
    if (ool.contains(OOL_SEPARATOR)) {
      sections = ool.trim().split(OOL_SEPARATOR_REGEX);
    } else {
      sections = new String[1];
      sections[0] = ool.trim();
    }
    for (final String section : sections) {
      if (section.length() == 0) {
        continue;
      }
      final String[] amountThenType = section.split(OOL_AMOUNT_DESCRIPTOR_REGEX);
      final int amount = amountThenType[0].equals(OOL_ALL) ? Integer.MAX_VALUE : Integer.parseInt(amountThenType[0]);
      final UnitType type = data.getUnitTypeList().getUnitType(amountThenType[1]);
      map.add(Tuple.of(amount, type));
    }
    Collections.reverse(map);
    final Collection<Unit> unitsLeft = new HashSet<>(units);
    final List<Unit> order = new ArrayList<>();
    for (final Tuple<Integer, UnitType> section : map) {
      final List<Unit> unitsOfType =
          Matches.getNMatches(unitsLeft, section.getFirst(), Matches.unitIsOfType(section.getSecond()));
      order.addAll(unitsOfType);
      unitsLeft.removeAll(unitsOfType);
    }
    Collections.reverse(order);
    return order;
  }
}
