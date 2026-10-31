package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import lombok.experimental.UtilityClass;

/** Casualty choices for ProAI that look past unit cost. */
@UtilityClass
public class ProCasualtyUtils {

  /**
   * Rough chance that a captured territory is still the captor's after the enemy's next turn, by
   * the combat plan's can-hold verdict.
   */
  static final double HOLD_CHANCE_CAN_HOLD = 0.75;

  static final double HOLD_CHANCE_TRADE = 0.2;
  static final double VICTORY_CITY_VALUE = 5;

  /**
   * What capturing a land territory is worth in PUs to the captor. Certain even if it's retaken:
   * its production (collected once) and a capital's treasury (taken at once, so the owner also
   * can't buy with it). A factory denies the owner its next placement there, worth up to its
   * production, unless an ally of the owner moving first can retake it, so then weighted by the
   * chance of holding it. Weighted by that chance too: a turn of the territory's income denied to
   * the owner, a victory city, and a capital owner's whole income.
   *
   * @param ownerIncome the owner's total production, which caps what it buys at the factory
   * @param ownerAllyMovesFirst whether an ally of the owner moves before the owner's next turn
   */
  public static double captureValue(
      final int production,
      final boolean factory,
      final boolean victoryCity,
      final boolean capital,
      final double treasury,
      final double ownerIncome,
      final boolean canHold,
      final boolean ownerAllyMovesFirst) {
    final double hold = canHold ? HOLD_CHANCE_CAN_HOLD : HOLD_CHANCE_TRADE;
    final double placementDenied =
        factory ? Math.min(production, ownerIncome) * (ownerAllyMovesFirst ? hold : 1) : 0;
    final double held =
        production + (victoryCity ? VICTORY_CITY_VALUE : 0) + (capital ? ownerIncome : 0);
    return production + (capital ? treasury : 0) + placementDenied + hold * held;
  }

  /** Keep {@code landToKeep} alive by losing {@code airToLose} instead. */
  public record KeepLandSwap(Unit landToKeep, Unit airToLose) {
    public List<Unit> apply(final Collection<Unit> killed) {
      final List<Unit> swapped = new ArrayList<>(killed);
      swapped.remove(landToKeep);
      swapped.add(airToLose);
      return swapped;
    }
  }

  /**
   * For an attacker on land: if {@code killed} takes every land unit still alive while air
   * survives, the swap that keeps the most valuable of those land units by losing the cheapest
   * surviving air unit that these hits may take. Only land units capture, so without it a won
   * battle leaves the territory to the defender.
   *
   * @param attackersAlive the attacker's units in the battle not yet marked as casualties,
   *     including {@code killed}
   * @param selectFrom the units these hits may take
   * @param damaged units these hits damage instead of kill; never also killed
   */
  public static Optional<KeepLandSwap> findKeepLandSwap(
      final Collection<Unit> killed,
      final Collection<Unit> attackersAlive,
      final Collection<Unit> selectFrom,
      final Collection<Unit> damaged,
      final ToDoubleFunction<Unit> value) {
    final List<Unit> survivors = new ArrayList<>(attackersAlive);
    survivors.removeAll(killed);
    if (survivors.stream().anyMatch(Matches.unitIsLand())) {
      return Optional.empty();
    }
    final Optional<Unit> landToKeep =
        killed.stream()
            .filter(Matches.unitIsLand())
            .filter(attackersAlive::contains)
            .max(Comparator.comparingDouble(value));
    final Optional<Unit> airToLose =
        survivors.stream()
            .filter(Matches.unitIsAir())
            .filter(selectFrom::contains)
            .filter(u -> !damaged.contains(u))
            .min(Comparator.comparingDouble(value));
    if (landToKeep.isEmpty() || airToLose.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new KeepLandSwap(landToKeep.get(), airToLose.get()));
  }
}
