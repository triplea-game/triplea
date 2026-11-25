package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TerritoryAttachment;
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

  /** What taking {@code site} is worth in PUs to {@code captor}, read from the game state. */
  public static double captureValue(
      final Territory site, final GamePlayer captor, final boolean canHold, final GameData data) {
    final Optional<TerritoryAttachment> ta = TerritoryAttachment.get(site);
    if (ta.isEmpty()) {
      return 0;
    }
    final GamePlayer owner = site.getOwner();
    boolean ownerAllyMovesFirst = false;
    for (final GamePlayer p : ProUtils.getOtherPlayersInTurnOrder(captor)) {
      if (p.equals(owner)) {
        break;
      }
      ownerAllyMovesFirst |= data.getRelationshipTracker().isAllied(p, owner);
    }
    final int production = ta.get().getProduction();
    return captureValue(
        production * Properties.getPuMultiplier(data.getProperties()),
        ProMatches.territoryHasInfraFactoryAndIsLand().test(site) ? production : 0,
        ta.get().getVictoryCity() > 0,
        ProUtils.capturedTreasury(site, data),
        ProUtils.captureStopsIncome(site, data.getMap()),
        ProUtils.getPlayerProduction(owner, data),
        canHold,
        ownerAllyMovesFirst);
  }

  /**
   * What capturing a land territory is worth in PUs to the captor. Certain even if it's retaken:
   * its income (collected once) and any treasury taken. A factory denies the owner its next
   * placement there, worth up to its capacity, unless an ally of the owner moving first can retake
   * it, so then weighted by the chance of holding it. Weighted by that chance too: a turn of the
   * territory's income denied to the owner, a victory city, and the owner's whole income if losing
   * this capital stops it collecting.
   *
   * @param factoryCapacity the units the owner could place here, 0 without a factory
   * @param ownerIncome the owner's total income, this territory's included, which caps what it buys
   *     at the factory
   * @param ownerAllyMovesFirst whether an ally of the owner moves before the owner's next turn
   */
  static double captureValue(
      final double income,
      final int factoryCapacity,
      final boolean victoryCity,
      final double treasuryTaken,
      final boolean deniesOwnerIncome,
      final double ownerIncome,
      final boolean canHold,
      final boolean ownerAllyMovesFirst) {
    final double hold = canHold ? HOLD_CHANCE_CAN_HOLD : HOLD_CHANCE_TRADE;
    final double placementDenied =
        Math.min(factoryCapacity, ownerIncome) * (ownerAllyMovesFirst ? hold : 1);
    final double held =
        (deniesOwnerIncome ? ownerIncome : income) + (victoryCity ? VICTORY_CITY_VALUE : 0);
    return income + treasuryTaken + placementDenied + hold * held;
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
