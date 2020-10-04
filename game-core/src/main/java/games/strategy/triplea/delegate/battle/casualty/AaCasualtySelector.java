package games.strategy.triplea.delegate.battle.casualty;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BaseEditDelegate;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.DiceRoll.TotalPowerAndTotalRolls;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.triplea.util.Triple;
import org.triplea.util.Tuple;

@UtilityClass
public class AaCasualtySelector {
  /** Choose plane casualties according to specified rules. */
  public static CasualtyDetails getAaCasualties(
      final boolean defending,
      final Collection<Unit> planes,
      final Collection<Unit> allFriendlyUnits,
      final Collection<Unit> defendingAa,
      final Collection<Unit> allEnemyUnits,
      final String text,
      final DiceRoll dice,
      final IDelegateBridge bridge,
      final GamePlayer hitPlayer,
      final UUID battleId,
      final Territory terr,
      final Collection<TerritoryEffect> territoryEffects) {
    if (planes.isEmpty()) {
      return new CasualtyDetails();
    }
    final GameData data = bridge.getData();
    final boolean allowMultipleHitsPerUnit =
        !defendingAa.isEmpty()
            && defendingAa.stream()
                .allMatch(Matches.unitAaShotDamageableInsteadOfKillingInstantly());
    if (BaseEditDelegate.getEditMode(data) || Properties.getChooseAaCasualties(data)) {
      return CasualtySelector.selectCasualties(
          hitPlayer,
          planes,
          allFriendlyUnits,
          allEnemyUnits,
          terr,
          territoryEffects,
          bridge,
          text,
          dice,
          defending,
          battleId,
          false,
          dice.getHits(),
          allowMultipleHitsPerUnit);
    }

    if (Properties.getLowLuck(data) || Properties.getLowLuckAaOnly(data)) {
      return getLowLuckAaCasualties(
          defending,
          planes,
          defendingAa,
          allEnemyUnits,
          allFriendlyUnits,
          dice,
          bridge,
          allowMultipleHitsPerUnit);
    }

    // priority goes: choose -> individually -> random
    // if none are set, we roll individually
    if (Properties.getRollAaIndividually(data)) {
      return individuallyFiredAaCasualties(
          defending,
          planes,
          defendingAa,
          allEnemyUnits,
          allFriendlyUnits,
          dice,
          bridge,
          allowMultipleHitsPerUnit);
    }
    if (Properties.getRandomAaCasualties(data)) {
      return randomAaCasualties(planes, dice, bridge, allowMultipleHitsPerUnit);
    }
    return individuallyFiredAaCasualties(
        defending,
        planes,
        defendingAa,
        allEnemyUnits,
        allFriendlyUnits,
        dice,
        bridge,
        allowMultipleHitsPerUnit);
  }

  private static CasualtyDetails getLowLuckAaCasualties(
      final boolean defending,
      final Collection<Unit> planes,
      final Collection<Unit> defendingAa,
      final Collection<Unit> allEnemyUnits,
      final Collection<Unit> allFriendlyUnits,
      final DiceRoll dice,
      final IDelegateBridge bridge,
      final boolean allowMultipleHitsPerUnit) {

    int hitsLeft = dice.getHits();
    if (hitsLeft <= 0) {
      return new CasualtyDetails();
    }

    final GameData data = bridge.getData();
    final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap =
        DiceRoll.getAaUnitPowerAndRollsForNormalBattles(
            defendingAa, allEnemyUnits, allFriendlyUnits, !defending, data);

    // if we can damage units, do it now
    final CasualtyDetails finalCasualtyDetails = new CasualtyDetails();
    final Tuple<Integer, Integer> attackThenDiceSides =
        DiceRoll.getMaxAaAttackAndDiceSides(defendingAa, data, !defending, unitPowerAndRollsMap);
    final int highestAttack = attackThenDiceSides.getFirst();
    if (highestAttack < 1) {
      return new CasualtyDetails();
    }
    final int chosenDiceSize = attackThenDiceSides.getSecond();
    final Triple<Integer, Integer, Boolean> triple =
        DiceRoll.getTotalAaPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(
            null, null, !defending, unitPowerAndRollsMap, planes, data, false);
    final boolean allSameAttackPower = triple.getThird();
    // multiple HP units need to be counted multiple times:
    final List<Unit> planesList = new ArrayList<>();
    for (final Unit plane : planes) {
      final int hpLeft =
          allowMultipleHitsPerUnit
              ? (UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits())
              : Math.min(1, UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits());
      for (int hp = 0; hp < hpLeft; ++hp) {
        // if allowMultipleHitsPerUnit, then because the number of rolls exactly equals the
        // hitpoints of all units,
        // we roll multiple times for any unit with multiple hitpoints
        planesList.add(plane);
      }
    }
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
        (int) Math.ceil((double) planesList.size() / (double) groupSize);
    final boolean tooManyHitsToDoGroups = hitsLeft > numberOfGroupsByDiceSides;
    if (!allSameAttackPower || tooManyHitsToDoGroups || chosenDiceSize % highestAttack != 0) {
      // we have too many hits, so just pick randomly
      return randomAaCasualties(planes, dice, bridge, allowMultipleHitsPerUnit);
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
        categorizeLowLuckAirUnits(planesList, groupSize);
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

  /** Choose plane casualties based on individual AA shots at each aircraft. */
  private static CasualtyDetails individuallyFiredAaCasualties(
      final boolean defending,
      final Collection<Unit> planes,
      final Collection<Unit> defendingAa,
      final Collection<Unit> allEnemyUnits,
      final Collection<Unit> allFriendlyUnits,
      final DiceRoll dice,
      final IDelegateBridge bridge,
      final boolean allowMultipleHitsPerUnit) {

    // if we have aa guns that are not infinite, then we need to randomly decide the aa casualties
    // since there are not
    // enough rolls to have a single roll for each aircraft, or too many rolls normal behavior is
    // instant kill, which
    // means planes.size()
    final int planeHitPoints =
        (allowMultipleHitsPerUnit ? CasualtyUtil.getTotalHitpointsLeft(planes) : planes.size());
    final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap =
        DiceRoll.getAaUnitPowerAndRollsForNormalBattles(
            defendingAa, allFriendlyUnits, allEnemyUnits, defending, bridge.getData());
    if (DiceRoll.getTotalAaAttacks(unitPowerAndRollsMap, planes) != planeHitPoints) {
      return randomAaCasualties(planes, dice, bridge, allowMultipleHitsPerUnit);
    }
    final Triple<Integer, Integer, Boolean> triple =
        DiceRoll.getTotalAaPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(
            null, null, !defending, unitPowerAndRollsMap, planes, bridge.getData(), false);
    final boolean allSameAttackPower = triple.getThird();
    if (!allSameAttackPower) {
      return randomAaCasualties(planes, dice, bridge, allowMultipleHitsPerUnit);
    }
    final int highestAttack =
        DiceRoll.getMaxAaAttackAndDiceSides(defendingAa, bridge.getData(), !defending).getFirst();
    final CasualtyDetails finalCasualtyDetails = new CasualtyDetails();
    final int hits = dice.getHits();
    final List<Unit> planesList = new ArrayList<>();
    for (final Unit plane : planes) {
      final int hpLeft =
          allowMultipleHitsPerUnit
              ? (UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits())
              : Math.min(1, UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits());
      for (int hp = 0; hp < hpLeft; ++hp) {
        // if allowMultipleHitsPerUnit, then because the number of rolls exactly equals the
        // hitpoints of all units,
        // we roll multiple times for any unit with multiple hitpoints
        planesList.add(plane);
      }
    }
    // We need to choose which planes die based on their position in the list and the individual AA
    // rolls
    if (hits > planeHitPoints) {
      throw new IllegalStateException("Cannot have more hits than number of die rolls");
    }
    if (hits < planeHitPoints) {
      final List<Die> rolls = dice.getRolls(highestAttack);
      for (int i = 0; i < rolls.size(); i++) {
        final Die die = rolls.get(i);
        if (die.getType() == DieType.HIT) {
          final Unit unit = planesList.get(i);
          if (allowMultipleHitsPerUnit
              && (Collections.frequency(finalCasualtyDetails.getDamaged(), unit)
                  < (getTotalHitpointsLeft(unit) - 1))) {
            finalCasualtyDetails.addToDamaged(unit);
          } else {
            finalCasualtyDetails.addToKilled(unit);
          }
        }
      }
    } else {
      for (final Unit plane : planesList) {
        if (finalCasualtyDetails.getKilled().contains(plane)) {
          finalCasualtyDetails.addToDamaged(plane);
        } else {
          finalCasualtyDetails.addToKilled(plane);
        }
      }
    }
    return finalCasualtyDetails;
  }

  /** Choose plane casualties randomly. */
  public static CasualtyDetails randomAaCasualties(
      final Collection<Unit> planes,
      final DiceRoll dice,
      final IDelegateBridge bridge,
      final boolean allowMultipleHitsPerUnit) {

    final int hitsLeft = dice.getHits();
    if (hitsLeft <= 0) {
      return new CasualtyDetails();
    }
    final CasualtyDetails finalCasualtyDetails = new CasualtyDetails();
    // normal behavior is instant kill, which means planes.size()
    final int planeHitPoints =
        (allowMultipleHitsPerUnit ? CasualtyUtil.getTotalHitpointsLeft(planes) : planes.size());
    final List<Unit> planesList = new ArrayList<>();
    for (final Unit plane : planes) {
      final int hpLeft =
          allowMultipleHitsPerUnit
              ? (UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits())
              : Math.min(1, UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits());
      for (int hp = 0; hp < hpLeft; ++hp) {
        // if allowMultipleHitsPerUnit, then because the number of rolls exactly equals the
        // hitpoints of all units,
        // we roll multiple times for any unit with multiple hitpoints
        planesList.add(plane);
      }
    }
    // We need to choose which planes die randomly
    if (hitsLeft < planeHitPoints) {
      // roll all at once to prevent frequent random calls, important for pbem games
      final int[] hitRandom =
          bridge.getRandom(
              planeHitPoints,
              hitsLeft,
              null,
              DiceType.ENGINE,
              "Deciding which planes should die due to AA fire");
      int pos = 0;
      for (final int element : hitRandom) {
        pos += element;
        final Unit unitHit = planesList.remove(pos % planesList.size());
        if (allowMultipleHitsPerUnit
            && (Collections.frequency(finalCasualtyDetails.getDamaged(), unitHit)
                < (getTotalHitpointsLeft(unitHit) - 1))) {
          finalCasualtyDetails.addToDamaged(unitHit);
        } else {
          finalCasualtyDetails.addToKilled(unitHit);
        }
      }
    } else {
      for (final Unit plane : planesList) {
        if (finalCasualtyDetails.getKilled().contains(plane)) {
          finalCasualtyDetails.addToDamaged(plane);
        } else {
          finalCasualtyDetails.addToKilled(plane);
        }
      }
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
