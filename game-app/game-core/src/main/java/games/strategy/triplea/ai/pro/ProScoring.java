package games.strategy.triplea.ai.pro;

import games.strategy.triplea.ai.pro.data.ProBattleResult;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import lombok.experimental.UtilityClass;

/** The arithmetic behind the Pro AI's choices, apart from the game state it reads. */
@UtilityClass
final class ProScoring {

  /** A candidate and what it scored. */
  record Scored<T>(T item, double score) {}

  /**
   * The first candidate with the lowest score. A score of positive infinity never wins, so a scorer
   * can rule a candidate out with it.
   */
  static <T> Optional<Scored<T>> lowestScoring(
      final Iterable<T> candidates, final ToDoubleFunction<T> scorer) {
    Scored<T> lowest = null;
    double lowestScore = Double.POSITIVE_INFINITY;
    for (final T candidate : candidates) {
      final double score = scorer.applyAsDouble(candidate);
      if (score < lowestScore) {
        lowestScore = score;
        lowest = new Scored<>(candidate, score);
      }
    }
    return Optional.ofNullable(lowest);
  }

  /** What a territory offers as a target for {@link #attackPriority}. */
  record AttackTarget(
      double tuvSwing,
      int production,
      boolean land,
      boolean neutral,
      boolean canHold,
      boolean amphib,
      boolean emptyLand,
      boolean enemyNextToMyCapital,
      boolean factory,
      boolean freeForAll,
      boolean capitalWorthTaking) {}

  /** Which territory to try to attack first; at or below 0 it isn't worth trying. */
  static double attackPriority(final AttackTarget target) {
    final double territoryValue =
        (1
                + bit(target.land())
                + bit(target.canHold()) * (1 + 2.0 * bit(target.freeForAll()) * bit(target.land())))
            * (1 + bit(target.emptyLand()))
            * (1 + bit(target.factory()))
            * (1 - 0.5 * bit(target.amphib()))
            * target.production();
    return (ownShareOfGain(target.tuvSwing(), target.freeForAll()) + territoryValue)
        * (1 + 4.0 * bit(target.capitalWorthTaking()))
        * (1 + 2.0 * bit(target.enemyNextToMyCapital()))
        * (1 - 0.9 * bit(target.neutral()));
  }

  /** A TUV gain counts half in a free-for-all, where a third player profits from it too. */
  private static double ownShareOfGain(final double tuvSwing, final boolean freeForAll) {
    return freeForAll && tuvSwing > 0 ? tuvSwing * 0.5 : tuvSwing;
  }

  /** What capturing a territory is worth to an attack already planned, before capital value. */
  static double attackedTerritoryValue(
      final boolean land,
      final boolean canHold,
      final boolean cantHoldAmphib,
      final boolean factory,
      final boolean freeForAll,
      final int capturableUnits,
      final int production) {
    return (1
            + bit(land)
            - bit(cantHoldAmphib)
            + bit(factory)
            + bit(canHold)
                * (1 + 2.0 * bit(freeForAll) + 1.5 * bit(factory) + 0.5 * capturableUnits))
        * production;
  }

  /** What a planned attack is worth: the fight, the odds of the territory, less the counter. */
  static double attackValue(
      final double tuvSwing,
      final boolean freeForAll,
      final double territoryValue,
      final double winPercentage,
      final double enemyCounterTuvSwing) {
    return 1
        + ownShareOfGain(tuvSwing, freeForAll)
        + territoryValue * winPercentage / 100
        - enemyCounterTuvSwing * 2 / 3;
  }

  /** Whether a planned attack should be dropped, apart from the odds of it capturing. */
  static boolean isNotWorthAttacking(
      final boolean neutral,
      final boolean canHold,
      final double attackValue,
      final boolean unitsCanAttackElsewhere,
      final double battleRounds) {
    return (neutral && !canHold)
        || (attackValue < 0 && (!neutral || unitsCanAttackElsewhere || battleRounds >= 4));
  }

  /** Whether a defense holds: no land unit survives to capture, or the capital's odds are good. */
  static boolean isHeld(
      final ProBattleResult result, final boolean isCapital, final double winPercentage) {
    return result.getTuvSwing() <= 0
        && (isCapital
            ? result.getWinPercentage() < 100 - winPercentage
            : !result.isHasLandUnitRemaining());
  }

  private static int bit(final boolean value) {
    return value ? 1 : 0;
  }

  /** How many turns a transport needs to reach a load territory; 0 when it arrives this turn. */
  static int turnsAway(final int distance, final int moves) {
    return distance <= moves ? 0 : (distance - 1) / moves;
  }

  /** Lower is a better place to load: close, valuable, few units waiting, little production. */
  static double loadValue(
      final double territoryValue,
      final int numTurnsAway,
      final int numUnitsToLoad,
      final int factoryProduction) {
    return territoryValue + 0.5 * numTurnsAway - 0.1 * numUnitsToLoad - 0.1 * factoryProduction;
  }

  /** Where a sea unit best goes: its sea value, mostly, multiplied by the transports it guards. */
  static double seaUnitValue(
      final int transports, final double seaValue, final double territoryValue) {
    return (1 + transports) * seaValue + (1 + transports * 100.0) * territoryValue / 10_000;
  }

  /**
   * How much a land territory needs amphibious units: empty transport capacity next to it outweighs
   * capacity two away, which outweighs how much sea it borders.
   */
  static int amphibUnitNeed(
      final int neighborTransportCapacity,
      final int nearbyTransportCapacity,
      final int transportCost,
      final boolean hasFactory,
      final int seaNeighbors) {
    final int neededNeighbor = Math.max(0, neighborTransportCapacity - transportCost);
    final int neededNearby =
        Math.max(0, neighborTransportCapacity + nearbyTransportCapacity - transportCost);
    return 1000 * neededNeighbor + 100 * neededNearby + (1 + 10 * bit(hasFactory)) * seaNeighbors;
  }

  /**
   * Where an air unit best lands: sea targets count most, then land, then enemies it can reach. A
   * territory that can't be held without allies counts for half, and a quarter without a factory.
   */
  static double airLandingValue(
      final int seaTargets,
      final int landTargets,
      final int enemyTargets,
      final int nearbyEnemies,
      final boolean cantHoldWithoutAllies,
      final boolean hasFactory,
      final boolean hasOwnedCarrier) {
    final int cantHold = bit(cantHoldWithoutAllies);
    return (200.0 * seaTargets + 100.0 * landTargets + 10.0 * enemyTargets + nearbyEnemies)
        / (1 + cantHold)
        / (1 + (double) cantHold * bit(!hasFactory))
        * (1 + bit(hasOwnedCarrier));
  }

  /**
   * A defense's TUV swing, credited for the value of the units added to make it. It is worth making
   * if this is no worse than the swing without them.
   */
  static double holdValue(final double tuvSwing, final double addedUnitValue, final boolean water) {
    return tuvSwing - (addedUnitValue / 8 * (1 + bit(water)));
  }

  /** Which territory to buy defenders for first; factories and the capital multiply the rest. */
  static double landDefensePriority(
      final int production,
      final boolean hasFactory,
      final double defendingUnitValue,
      final boolean isMyCapital) {
    final int factory = bit(hasFactory);
    return (2.0 * production + 4.0 * factory + 0.5 * defendingUnitValue)
        * (1 + factory)
        * (1 + 10.0 * bit(isMyCapital));
  }

  /**
   * Which sea zone to buy for first: more transports and warships raise it, needing help cuts it.
   */
  static double seaPurchasePriority(
      final double strategicValue,
      final int transports,
      final int seaDefenders,
      final boolean needsDefenders) {
    return strategicValue * (1 + transports + 0.1 * seaDefenders) / (1 + 3.0 * bit(needsDefenders));
  }
}
