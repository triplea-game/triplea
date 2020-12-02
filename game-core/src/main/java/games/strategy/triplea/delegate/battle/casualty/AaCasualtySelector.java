package games.strategy.triplea.delegate.battle.casualty;

import com.google.common.collect.Lists;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BaseEditDelegate;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.power.calculator.AaPowerStrengthAndRolls;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Value;
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
    final GameData data = bridge.getData();
    final boolean allowMultipleHitsPerUnit =
        !defendingAa.isEmpty()
            && defendingAa.stream()
                .allMatch(Matches.unitAaShotDamageableInsteadOfKillingInstantly());
    if (BaseEditDelegate.getEditMode(data)
        || Properties.getChooseAaCasualties(data.getProperties())) {
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

    return Properties.getLowLuck(data.getProperties())
            || Properties.getLowLuckAaOnly(data.getProperties())
        ? getLowLuckAaCasualties(availableTargets, unitPowerAndRollsMap, dice, bridge)
        : calculateRolledAaCasualties(availableTargets, unitPowerAndRollsMap, dice, bridge);
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
              ? (UnitAttachment.get(target.getType()).getHitPoints() - target.getHits())
              : Math.min(1, UnitAttachment.get(target.getType()).getHitPoints() - target.getHits());
      for (int hp = 0; hp < hpLeft; ++hp) {
        // if allowMultipleHitsPerUnit, then the target needs to be added for each hp
        targetsList.add(target);
      }
    }
    return targetsList;
  }

  private static CasualtyDetails getLowLuckAaCasualties(
      final List<Unit> availableTargets,
      final AaPowerStrengthAndRolls unitPowerAndRollsMap,
      final DiceRoll dice,
      final IDelegateBridge bridge) {

    final LowLuckTargetGroups targetGroups =
        createGuaranteedLowLuckHitGroups(availableTargets, dice, unitPowerAndRollsMap);

    if (targetGroups.guaranteedHitGroups.isEmpty()) {
      // it is not possible to separate the targets into guaranteed hit groups so randomly choose
      // the targets instead
      return buildCasualtyDetails(
          availableTargets, findRandomTargets(availableTargets, bridge, dice.getHits()));
    }

    if (dice.getHits() >= targetGroups.guaranteedHitGroups.size()) {
      // there are enough hits to take one unit from each guaranteed hit group
      final List<Unit> hitTargetIndices = new ArrayList<>();
      for (final List<Unit> group : targetGroups.guaranteedHitGroups) {
        hitTargetIndices.add(group.get(0));
      }

      // if there are more hits than groups, the extra hits come out of the remainderUnits
      final int remainderHits = dice.getHits() - targetGroups.guaranteedHitGroups.size();
      if (remainderHits > 0) {
        if (remainderHits == targetGroups.remainderUnits.size()) {
          hitTargetIndices.addAll(targetGroups.remainderUnits);
        } else {
          // randomly pull out units from the remainder group
          hitTargetIndices.addAll(
              findRandomTargets(targetGroups.remainderUnits, bridge, remainderHits));
        }
      }
      return buildCasualtyDetails(availableTargets, hitTargetIndices);
    } else {
      // There is somehow more guaranteed hit groups than hits. This currently only happens
      // with multi hp targets and damageable AA shots.

      // pull out one unit from each guaranteed hit and then randomly pick the hits from those
      final List<Unit> guaranteedHitUnits = new ArrayList<>();
      for (final List<Unit> group : targetGroups.guaranteedHitGroups) {
        guaranteedHitUnits.add(group.get(0));
      }

      return buildCasualtyDetails(
          availableTargets, findRandomTargets(guaranteedHitUnits, bridge, dice.getHits()));
    }
  }

  /**
   * Categorize the units and then split them up into groups of guaranteeHitGroupSize
   *
   * <p>Any group less than guaranteeHitGroupSize is added to the remainderUnits
   */
  private static LowLuckTargetGroups createGuaranteedLowLuckHitGroups(
      final Collection<Unit> targets,
      final DiceRoll diceRoll,
      final AaPowerStrengthAndRolls unitPowerAndRollsMap) {

    final int guaranteeHitGroupSize =
        calculateGuaranteeLowLuckHitGroupSize(targets, diceRoll, unitPowerAndRollsMap);

    final Collection<UnitCategory> groupedTargets =
        UnitSeparator.categorize(targets, null, false, true);
    final List<List<Unit>> guaranteedHitGroups = new ArrayList<>();
    final List<Unit> remainderUnits = new ArrayList<>();
    for (final UnitCategory uc : groupedTargets) {
      final Deque<List<Unit>> guaranteedGroups =
          new ArrayDeque<>(Lists.partition(uc.getUnits(), guaranteeHitGroupSize));
      final List<Unit> lastGroup = guaranteedGroups.peekLast();
      // if the last group isn't the right size, put those units in the remainder list
      if (lastGroup != null && lastGroup.size() != guaranteeHitGroupSize) {
        guaranteedGroups.removeLast();
        remainderUnits.addAll(lastGroup);
      }
      guaranteedHitGroups.addAll(guaranteedGroups);
    }
    return LowLuckTargetGroups.of(guaranteedHitGroups, remainderUnits);
  }

  @Value(staticConstructor = "of")
  private static class LowLuckTargetGroups {
    List<List<Unit>> guaranteedHitGroups;
    List<Unit> remainderUnits;
  }

  /**
   * Calculate the number of targets that guarantee a hit in a low luck dice roll
   *
   * <p>In low luck, the number of hits = (power / dice sides). If the strength for all of the aa
   * units is the same, then the number of hits = (strength * targetCount / diceSides). To find out
   * how big a group is needed to get a guaranteed hit, re-order the equation to be (targetCount /
   * hits) = (diceSides / strength). So, for every (diceSides / strength) targets, there is one
   * guaranteed hit.
   *
   * @return 0 if not possible to split up the targets into guaranteed hit groups or > 0 for the
   *     size that guarantees a hit
   */
  private static int calculateGuaranteeLowLuckHitGroupSize(
      final Collection<Unit> availableTargets,
      final DiceRoll diceRoll,
      final AaPowerStrengthAndRolls unitPowerAndRollsMap) {
    final int bestStrength = unitPowerAndRollsMap.getBestStrength();
    final int chosenDiceSize = unitPowerAndRollsMap.getDiceSides();

    final boolean hasOverstackHits =
        diceRoll.getHits()
            > Math.ceil(
                (double) (bestStrength * availableTargets.size()) / (double) chosenDiceSize);

    // if the aa units aren't the same strength, then it isn't possible to calculate the target
    // count for a guaranteed hit because different strengths have different target counts.
    if (!unitPowerAndRollsMap.isSameStrength()
        // if there are more hits than (strength * targetCount / diceSides), then there must have
        // been overstack AA and that messes up the target count.
        || hasOverstackHits
        // if the best strength isn't a factor of chosenDiceSize, then the target count will be
        // fractional which doesn't work
        || chosenDiceSize % bestStrength != 0) {
      return 0;
    }

    return chosenDiceSize / bestStrength;
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

  private static CasualtyDetails calculateRolledAaCasualties(
      final List<Unit> availableTargets,
      final AaPowerStrengthAndRolls unitPowerAndRollsMap,
      final DiceRoll dice,
      final IDelegateBridge bridge) {

    final int hits = dice.getHits();
    final Collection<Unit> hitTargets;
    if (unitPowerAndRollsMap.calculateTotalRolls() == availableTargets.size()
        && hits < availableTargets.size()) {
      // there is a roll for every target but not enough hits to kill all of the targets
      // so no need to get a random set of units since all units will either have a hit
      // or miss roll
      hitTargets = findRolledTargets(availableTargets, dice);
    } else if (hits < availableTargets.size()) {
      // there isn't a roll for every target so need to randomly pick the target for each hit
      hitTargets = findRandomTargets(availableTargets, bridge, hits);
    } else {
      // all targets were hit so add them all
      hitTargets = availableTargets;
    }

    return buildCasualtyDetails(availableTargets, hitTargets);
  }

  /** Find the targets that were hit by a dice roll */
  private static Collection<Unit> findRolledTargets(
      final List<Unit> availableTargets, final DiceRoll dice) {
    final List<Die> rolls = dice.getRolls();
    final List<Unit> hitTargets = new ArrayList<>();
    for (int i = 0; i < rolls.size(); i++) {
      if (rolls.get(i).getType() == DieType.HIT) {
        hitTargets.add(availableTargets.get(i));
      }
    }
    return hitTargets;
  }
}
