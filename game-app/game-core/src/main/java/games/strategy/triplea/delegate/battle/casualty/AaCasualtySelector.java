package games.strategy.triplea.delegate.battle.casualty;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.power.calculator.AaPowerStrengthAndRolls;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AaCasualtySelector {
  /** Choose plane casualties according to specified rules. */
  public static CasualtyDetails getAaCasualties(
      final Collection<Unit> planes,
      final Collection<Unit> defendingAa,
      final CombatValue planesCombatValueCalculator,
      final CombatValue aaCombatValueCalculator,
      final String text,
      final DiceRoll dice,
      final IDelegateBridge bridge,
      final GamePlayer hitPlayer,
      final UUID battleId,
      final Territory battleSite) {
    if (planes.isEmpty()) {
      return new CasualtyDetails();
    }
    final GameState data = bridge.getData();
    final boolean allowMultipleHitsPerUnit =
        !defendingAa.isEmpty()
            && defendingAa.stream()
                .allMatch(Matches.unitAaShotDamageableInsteadOfKillingInstantly());
    if (Properties.getChooseAaCasualties(data.getProperties())) {
      return CasualtySelector.selectCasualties(
          hitPlayer,
          planes,
          planesCombatValueCalculator,
          battleSite,
          bridge,
          text,
          dice,
          battleId,
          false,
          dice.getHits(),
          allowMultipleHitsPerUnit);
    }

    if (dice.getHits() <= 0) {
      return new CasualtyDetails();
    }

    final AaPowerStrengthAndRolls unitPowerAndRollsMap =
        AaPowerStrengthAndRolls.build(defendingAa, planes.size(), aaCombatValueCalculator);
    final List<Unit> availableTargets = calculateAvailableTargets(planes, allowMultipleHitsPerUnit);

    return buildCasualtyDetails(
        availableTargets,
        Properties.getLowLuck(data.getProperties())
                || Properties.getLowLuckAaOnly(data.getProperties())
            ? getLowLuckAaCasualties(availableTargets, unitPowerAndRollsMap, dice, bridge)
            : calculateRolledAaCasualties(availableTargets, unitPowerAndRollsMap, dice, bridge));
  }

  /**
   * Calculate a list of targets that can be shot at
   *
   * @param allowMultipleHitsPerUnit if true, the targets will be increased to include duplicate
   *     targets to handle their extra hit points
   * @return A list of targets (that may contain duplicate) that can be shot at
   */
  private static List<Unit> calculateAvailableTargets(
      final Collection<Unit> targets, final boolean allowMultipleHitsPerUnit) {
    final List<Unit> targetsList = new ArrayList<>();
    for (final Unit target : targets) {
      final int hpLeft =
          allowMultipleHitsPerUnit
              ? (target.getUnitAttachment().getHitPoints() - target.getHits())
              : Math.min(1, target.getUnitAttachment().getHitPoints() - target.getHits());
      for (int hp = 0; hp < hpLeft; ++hp) {
        // if allowMultipleHitsPerUnit, then the target needs to be added for each hp
        targetsList.add(target);
      }
    }
    return targetsList;
  }

  private static CasualtyDetails buildCasualtyDetails(
      final List<Unit> availableTargets, final Collection<Unit> hitTargets) {
    final Map<Unit, Long> unitHp =
        availableTargets.stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    final CasualtyDetails casualtyDetails = new CasualtyDetails();
    for (final Unit hitTarget : hitTargets) {
      final Unit unit = availableTargets.get(availableTargets.indexOf(hitTarget));
      unitHp.computeIfPresent(
          unit,
          (unitKey, hp) -> {
            if (hp > 1) {
              casualtyDetails.addToDamaged(unit);
            } else {
              casualtyDetails.addToKilled(unit);
            }
            return hp - 1;
          });
    }
    return casualtyDetails;
  }

  private static Collection<Unit> getLowLuckAaCasualties(
      final List<Unit> availableTargets,
      final AaPowerStrengthAndRolls unitPowerAndRollsMap,
      final DiceRoll dice,
      final IDelegateBridge bridge) {

    final LowLuckTargetGroups targetGroups =
        new LowLuckTargetGroups(availableTargets, dice, unitPowerAndRollsMap);

    if (!targetGroups.hasGuaranteedGroups()) {
      // it is not possible to separate the targets into guaranteed hit groups so randomly choose
      // the targets instead
      return findRandomTargets(availableTargets, bridge, dice.getHits());
    }

    if (dice.getHits() >= targetGroups.getGuaranteedHitGroups().size()) {
      // there are enough hits to hit all of the guaranteed hits
      final List<Unit> hitUnits = targetGroups.getGuaranteedHits();

      // if there are more hits than groups, the extra hits come out of the remainderUnits
      final int remainderHits = dice.getHits() - hitUnits.size();
      if (remainderHits > 0) {
        if (remainderHits == targetGroups.getRemainderUnits().size()) {
          hitUnits.addAll(targetGroups.getRemainderUnits());
        } else {
          // randomly pull out units from the remainder group
          hitUnits.addAll(
              findRandomTargets(targetGroups.getRemainderUnits(), bridge, remainderHits));
        }
      }
      return hitUnits;
    } else {
      // There is somehow more guaranteed hit groups than hits. This currently only happens
      // with multi hp targets and damageable AA shots.

      // Randomly pick out of the guaranteed hits
      return findRandomTargets(targetGroups.getGuaranteedHits(), bridge, dice.getHits());
    }
  }

  /** Select a random set of targets out of availableTargets */
  private static Collection<Unit> findRandomTargets(
      final List<Unit> availableTargets, final IDelegateBridge bridge, final int hits) {
    final int[] hitRandom =
        bridge.getRandom(
            availableTargets.size(),
            hits,
            null,
            DiceType.ENGINE,
            "Deciding which planes should die due to AA fire");
    // turn the random numbers into a unique set of targets
    final Set<Integer> hitTargets = new HashSet<>();
    int index = 0;
    for (final int randomIndex : hitRandom) {
      index = (index + randomIndex) % availableTargets.size();
      while (hitTargets.contains(index)) {
        index = (index + 1) % availableTargets.size();
      }
      hitTargets.add(index);
    }
    return hitTargets.stream().map(availableTargets::get).collect(Collectors.toList());
  }

  private static Collection<Unit> calculateRolledAaCasualties(
      final List<Unit> availableTargets,
      final AaPowerStrengthAndRolls unitPowerAndRollsMap,
      final DiceRoll dice,
      final IDelegateBridge bridge) {

    if (unitPowerAndRollsMap.calculateTotalRolls() == availableTargets.size()
        && dice.getHits() < availableTargets.size()) {
      // there is a roll for every target but not enough hits to kill all of the targets
      // so no need to get a random set of units since all units will either have a hit
      // or miss roll
      return findRolledTargets(availableTargets, dice);
    } else if (dice.getHits() < availableTargets.size()) {
      // there isn't a roll for every target so need to randomly pick the target for each hit
      return findRandomTargets(availableTargets, bridge, dice.getHits());
    } else {
      // all targets were hit so add them all
      return availableTargets;
    }
  }

  /** Find the targets that were hit by a dice roll */
  private static Collection<Unit> findRolledTargets(
      final List<Unit> availableTargets, final DiceRoll dice) {
    Preconditions.checkArgument(
        availableTargets.size() == dice.getRolls().size(),
        "findRolledTargets needs one roll per target");

    return IntStream.range(0, dice.getRolls().size())
        .filter(rollIdx -> dice.getRolls().get(rollIdx).getType() == DieType.HIT)
        .mapToObj(availableTargets::get)
        .collect(Collectors.toList());
  }
}
