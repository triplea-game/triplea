package games.strategy.triplea.delegate.battle.casualty;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;
import org.triplea.util.Tuple;

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

    if (Properties.getLowLuck(data.getProperties())
        || Properties.getLowLuckAaOnly(data.getProperties())) {
      return getLowLuckAaCasualties(
          availableTargets, unitPowerAndRollsMap, dice, bridge, allowMultipleHitsPerUnit);
    }

    return calculateAaCasualties(availableTargets, unitPowerAndRollsMap, dice, bridge);
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
      final IDelegateBridge bridge,
      final boolean allowMultipleHitsPerUnit) {

    int hitsLeft = dice.getHits();

    // if we can damage units, do it now
    final CasualtyDetails finalCasualtyDetails = new CasualtyDetails();
    final int highestAttack = unitPowerAndRollsMap.getBestStrength();
    if (highestAttack < 1) {
      return new CasualtyDetails();
    }
    final int chosenDiceSize = unitPowerAndRollsMap.getDiceSides();
    final boolean allSameAttackPower = unitPowerAndRollsMap.isSameStrength();
    // multiple HP units need to be counted multiple times:
    // killing the air by groups does not work if the the attack power is different for some of the
    // rolls
    // also, killing by groups does not work if some of the aa guns have 'MayOverStackAA' and we
    // have more hits than the
    // total number of groups (including the remainder group)
    // (when i mean, 'does not work', i mean that it is no longer a mathematically fair way to find
    // casualties)
    // find group size (if no groups, do dice sides)
    final int groupSize;
    if (allSameAttackPower) {
      groupSize = chosenDiceSize / highestAttack;
    } else {
      groupSize = chosenDiceSize;
    }
    final int numberOfGroupsByDiceSides =
        (int) Math.ceil((double) availableTargets.size() / (double) groupSize);
    final boolean tooManyHitsToDoGroups = hitsLeft > numberOfGroupsByDiceSides;
    if (!allSameAttackPower || tooManyHitsToDoGroups || chosenDiceSize % highestAttack != 0) {
      // we have too many hits, so just pick randomly
      return calculateAaCasualties(availableTargets, unitPowerAndRollsMap, dice, bridge);
    }

    // if we have a group of 6 fighters and 2 bombers, and dicesides is 6, and attack was 1, then we
    // would want 1
    // fighter to die for sure. this is what group size is for.
    // if the attack is greater than 1 though, and all use the same attack power, then the group
    // size can be smaller
    // (ie: attack is 2, and we have 3 fighters and 2 bombers, we would want 1 fighter to die for
    // sure).
    // categorize with groupSize
    final Tuple<List<List<Unit>>, List<Unit>> airSplit =
        categorizeLowLuckAirUnits(availableTargets, groupSize);
    // the non rolling air units
    // if we are less hits than the number of groups, OR we have equal hits to number of groups but
    // we also have a
    // remainder that is equal to or greater than group size,
    // THEN we need to make sure to pick randomly, and include the remainder group. (reason we do
    // not do this with any
    // remainder size, is because we might have missed the dice roll to hit the remainder)
    if (hitsLeft
        < (airSplit.getFirst().size()
            + ((int) Math.ceil((double) airSplit.getSecond().size() / (double) groupSize)))) {
      // fewer hits than groups
      final List<Unit> tempPossibleHitUnits = new ArrayList<>();
      for (final List<Unit> group : airSplit.getFirst()) {
        tempPossibleHitUnits.add(group.get(0));
      }
      if (!airSplit.getSecond().isEmpty()) {
        // if we have a remainder group, we need to add some of them into the mix
        // but we have to do so randomly
        final List<Unit> remainders = new ArrayList<>(airSplit.getSecond());
        if (remainders.size() == 1) {
          tempPossibleHitUnits.add(remainders.remove(0));
        } else {
          final int numberOfRemainderGroups =
              (int) Math.ceil((double) remainders.size() / (double) groupSize);
          final int[] randomRemainder =
              bridge.getRandom(
                  remainders.size(),
                  numberOfRemainderGroups,
                  null,
                  DiceType.ENGINE,
                  "Deciding which planes should die due to AA fire");
          int pos2 = 0;
          for (final int element : randomRemainder) {
            pos2 += element;
            tempPossibleHitUnits.add(remainders.remove(pos2 % remainders.size()));
          }
        }
      }
      final int[] hitRandom =
          bridge.getRandom(
              tempPossibleHitUnits.size(),
              hitsLeft,
              null,
              DiceType.ENGINE,
              "Deciding which planes should die due to AA fire");
      // now we find the
      int pos = 0;
      for (final int element : hitRandom) {
        pos += element;
        final Unit unitHit = tempPossibleHitUnits.remove(pos % tempPossibleHitUnits.size());
        if (allowMultipleHitsPerUnit
            && (Collections.frequency(finalCasualtyDetails.getDamaged(), unitHit)
                < (getTotalHitpointsLeft(unitHit) - 1))) {
          finalCasualtyDetails.addToDamaged(unitHit);
        } else {
          finalCasualtyDetails.addToKilled(unitHit);
        }
      }
    } else {
      // kill one in every group
      for (final List<Unit> group : airSplit.getFirst()) {
        final Unit unitHit = group.get(0);
        if (allowMultipleHitsPerUnit
            && (Collections.frequency(finalCasualtyDetails.getDamaged(), unitHit)
                < (getTotalHitpointsLeft(unitHit) - 1))) {
          finalCasualtyDetails.addToDamaged(unitHit);
        } else {
          finalCasualtyDetails.addToKilled(unitHit);
        }
        hitsLeft--;
      }
      // for any hits left over...
      if (hitsLeft == airSplit.getSecond().size()) {
        for (final Unit unitHit : airSplit.getSecond()) {
          if (allowMultipleHitsPerUnit
              && (Collections.frequency(finalCasualtyDetails.getDamaged(), unitHit)
                  < (getTotalHitpointsLeft(unitHit) - 1))) {
            finalCasualtyDetails.addToDamaged(unitHit);
          } else {
            finalCasualtyDetails.addToKilled(unitHit);
          }
        }
      } else if (hitsLeft != 0) {
        // the remainder
        // roll all at once to prevent frequent random calls, important for pbem games
        final int[] hitRandom =
            bridge.getRandom(
                airSplit.getSecond().size(),
                hitsLeft,
                null,
                DiceType.ENGINE,
                "Deciding which planes should die due to AA fire");
        int pos = 0;
        for (final int element : hitRandom) {
          pos += element;
          final Unit unitHit = airSplit.getSecond().remove(pos % airSplit.getSecond().size());
          if (allowMultipleHitsPerUnit
              && (Collections.frequency(finalCasualtyDetails.getDamaged(), unitHit)
                  < (getTotalHitpointsLeft(unitHit) - 1))) {
            finalCasualtyDetails.addToDamaged(unitHit);
          } else {
            finalCasualtyDetails.addToKilled(unitHit);
          }
        }
      }
    }

    // double check
    if (finalCasualtyDetails.size() != dice.getHits()) {
      throw new IllegalStateException(
          "wrong number of casualties, expected:" + dice + " but got: " + finalCasualtyDetails);
    }
    return finalCasualtyDetails;
  }

  private static CasualtyDetails calculateAaCasualties(
      final List<Unit> availableTargets,
      final AaPowerStrengthAndRolls unitPowerAndRollsMap,
      final DiceRoll dice,
      final IDelegateBridge bridge) {

    final CasualtyDetails finalCasualtyDetails = new CasualtyDetails();
    final int hits = dice.getHits();
    final Set<Integer> hitTargets;
    if (unitPowerAndRollsMap.calculateTotalRolls() == availableTargets.size()
        && hits < availableTargets.size()) {
      // there is a roll for every target but not enough hits to kill all of the targets
      // so no need to get a random set of units since all units will either have a hit
      // or miss roll
      final List<Die> rolls = dice.getRolls(unitPowerAndRollsMap.getBestStrength());
      hitTargets = new HashSet<>();
      for (int i = 0; i < rolls.size(); i++) {
        if (rolls.get(i).getType() == DieType.HIT) {
          hitTargets.add(i);
        }
      }
    } else if (hits < availableTargets.size()) {
      // there isn't a roll for every target so need to randomly pick the target for each hit
      final int[] hitRandom =
          bridge.getRandom(
              availableTargets.size(),
              hits,
              null,
              DiceType.ENGINE,
              "Deciding which planes should die due to AA fire");
      // turn the random numbers into a unique set of targets
      hitTargets = new HashSet<>();
      int index = 0;
      for (final int randomIndex : hitRandom) {
        index = (index + randomIndex) % availableTargets.size();
        while (hitTargets.contains(index)) {
          index = (index + 1) % availableTargets.size();
        }
        hitTargets.add(index);
      }
    } else {
      // all targets were hit so add them all
      hitTargets = IntStream.range(0, availableTargets.size()).boxed().collect(Collectors.toSet());
    }

    final Map<Unit, Long> unitHp =
        availableTargets.stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    for (final Integer hitTarget : hitTargets) {
      final Unit unit = availableTargets.get(hitTarget);
      unitHp.computeIfPresent(
          unit,
          (unitKey, hp) -> {
            if (hp > 1) {
              finalCasualtyDetails.addToDamaged(unit);
            } else {
              finalCasualtyDetails.addToKilled(unit);
            }
            return hp - 1;
          });
    }
    return finalCasualtyDetails;
  }

  /**
   * http://triplea.sourceforge.net/mywiki/Forum#nabble-td4658925%7Ca4658925 returns two lists, the
   * first list is the air units that can be evenly divided into groups of 3 or 6 (depending on
   * radar) the second list is all the air units that do not fit in the first list
   */
  private static Tuple<List<List<Unit>>, List<Unit>> categorizeLowLuckAirUnits(
      final Collection<Unit> units, final int groupSize) {
    final Collection<UnitCategory> categorizedAir =
        UnitSeparator.categorize(units, null, false, true);
    final List<List<Unit>> groupsOfSize = new ArrayList<>();
    final List<Unit> toRoll = new ArrayList<>();
    for (final UnitCategory uc : categorizedAir) {
      final int remainder = uc.getUnits().size() % groupSize;
      final int splitPosition = uc.getUnits().size() - remainder;
      final List<Unit> group = new ArrayList<>(uc.getUnits().subList(0, splitPosition));
      if (!group.isEmpty()) {
        for (int i = 0; i < splitPosition; i += groupSize) {
          final List<Unit> miniGroup = new ArrayList<>(uc.getUnits().subList(i, i + groupSize));
          if (!miniGroup.isEmpty()) {
            groupsOfSize.add(miniGroup);
          }
        }
      }
      toRoll.addAll(uc.getUnits().subList(splitPosition, uc.getUnits().size()));
    }
    return Tuple.of(groupsOfSize, toRoll);
  }

  private static int getTotalHitpointsLeft(final Unit unit) {
    if (unit == null) {
      return 0;
    }
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    return ua.getHitPoints() - unit.getHits();
  }
}
