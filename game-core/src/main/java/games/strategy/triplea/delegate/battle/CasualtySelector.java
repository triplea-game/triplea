package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.Player;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BaseEditDelegate;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.UnitComparator;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import lombok.extern.java.Log;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Triple;
import org.triplea.util.Tuple;

/**
 * Utility class for determining casualties and selecting casualties. The code was being duplicated
 * all over the place.
 */
@Log
public class CasualtySelector {
  private static final Map<String, List<UnitType>> oolCache = new ConcurrentHashMap<>();

  private CasualtySelector() {}

  public static void clearOolCache() {
    oolCache.clear();
  }

  /**
   * Sort in a determined way so that the dice results appear in a logical order. Also sort by
   * movement, so casualties will be chosen as the units with least movement.
   */
  static void sortPreBattle(final List<Unit> units) {
    units.sort(
        Comparator.comparing(Unit::getType, Comparator.comparing(UnitType::getName))
            .thenComparing(UnitComparator.getLowestToHighestMovementComparator()));
  }

  /**
   * In an amphibious assault, sort on who is unloading from transports first as this will allow the
   * marines with higher scores to get killed last.
   */
  public static void sortAmphib(final List<Unit> units, final List<Unit> amphibiousLandAttackers) {
    final Comparator<Unit> decreasingMovement =
        UnitComparator.getLowestToHighestMovementComparator();
    units.sort(
        Comparator.comparing(Unit::getType, Comparator.comparing(UnitType::getName))
            .thenComparing(
                (u1, u2) -> {
                  final UnitAttachment ua = UnitAttachment.get(u1.getType());
                  final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
                  if (ua.getIsMarine() != 0 && ua2.getIsMarine() != 0) {
                    return compareAccordingToAmphibious(u1, u2, amphibiousLandAttackers);
                  }
                  return 0;
                })
            .thenComparing(decreasingMovement));
  }

  private static int compareAccordingToAmphibious(
      final Unit u1, final Unit u2, final List<Unit> amphibiousLandAttackers) {
    if (amphibiousLandAttackers.contains(u1) && !amphibiousLandAttackers.contains(u2)) {
      return -1;
    } else if (amphibiousLandAttackers.contains(u2) && !amphibiousLandAttackers.contains(u1)) {
      return 1;
    }
    final int m1 = UnitAttachment.get(u1.getType()).getIsMarine();
    final int m2 = UnitAttachment.get(u2.getType()).getIsMarine();
    return m2 - m1;
  }

  /** Find total remaining hit points of units. */
  public static int getTotalHitpointsLeft(final Collection<Unit> units) {
    if (units == null || units.isEmpty()) {
      return 0;
    }
    int totalHitPoints = 0;
    for (final Unit u : units) {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      if (!ua.getIsInfrastructure()) {
        totalHitPoints += ua.getHitPoints();
        totalHitPoints -= u.getHits();
      }
    }
    return totalHitPoints;
  }

  private static int getTotalHitpointsLeft(final Unit unit) {
    if (unit == null) {
      return 0;
    }
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    return ua.getHitPoints() - unit.getHits();
  }

  /** Choose plane casualties according to specified rules. */
  public static CasualtyDetails getAaCasualties(
      final boolean defending,
      final Collection<Unit> planes,
      final Collection<Unit> allFriendlyUnits,
      final Collection<Unit> defendingAa,
      final Collection<Unit> allEnemyUnits,
      final DiceRoll dice,
      final IDelegateBridge bridge,
      final GamePlayer hitPlayer,
      final UUID battleId,
      final Territory terr,
      final Collection<TerritoryEffect> territoryEffects,
      final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers) {
    if (planes.isEmpty()) {
      return new CasualtyDetails();
    }
    final GameData data = bridge.getData();
    final boolean allowMultipleHitsPerUnit =
        !defendingAa.isEmpty()
            && defendingAa.stream()
                .allMatch(Matches.unitAaShotDamageableInsteadOfKillingInstantly());
    if (isChooseAa(data)) {
      final String text =
          "Select " + dice.getHits() + " casualties from aa fire in " + terr.getName();
      return selectCasualties(
          hitPlayer,
          planes,
          allFriendlyUnits,
          allEnemyUnits,
          amphibious,
          amphibiousLandAttackers,
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
    if (isRollAaIndividually(data)) {
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
    if (isRandomAaCasualties(data)) {
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
    final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRollsMap =
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
        (allowMultipleHitsPerUnit ? getTotalHitpointsLeft(planes) : planes.size());
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
        (allowMultipleHitsPerUnit ? getTotalHitpointsLeft(planes) : planes.size());
    final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRollsMap =
        DiceRoll.getAaUnitPowerAndRollsForNormalBattles(
            defendingAa, allEnemyUnits, allFriendlyUnits, defending, bridge.getData());
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

  /**
   * Selects casualties for the specified battle.
   *
   * @param battleId may be null if we are not in a battle (eg, if this is an aa fire due to
   *     moving).
   */
  public static CasualtyDetails selectCasualties(
      final GamePlayer player,
      final Collection<Unit> targetsToPickFrom,
      final Collection<Unit> friendlyUnits,
      final Collection<Unit> enemyUnits,
      final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers,
      final Territory battlesite,
      final Collection<TerritoryEffect> territoryEffects,
      final IDelegateBridge bridge,
      final String text,
      final DiceRoll dice,
      final boolean defending,
      final UUID battleId,
      final boolean headLess,
      final int extraHits,
      final boolean allowMultipleHitsPerUnit) {
    if (targetsToPickFrom.isEmpty()) {
      return new CasualtyDetails();
    }
    if (!friendlyUnits.containsAll(targetsToPickFrom)) {
      throw new IllegalStateException(
          "friendlyUnits should but does not contain all units from targetsToPickFrom");
    }
    final GameData data = bridge.getData();
    final boolean isEditMode = BaseEditDelegate.getEditMode(data);
    final Player tripleaPlayer =
        player.isNull() ? new WeakAi(player.getName()) : bridge.getRemotePlayer(player);
    final Map<Unit, Collection<Unit>> dependents =
        headLess ? Map.of() : getDependents(targetsToPickFrom);
    if (isEditMode && !headLess) {
      final CasualtyDetails editSelection =
          tripleaPlayer.selectCasualties(
              targetsToPickFrom,
              dependents,
              0,
              text,
              dice,
              player,
              friendlyUnits,
              enemyUnits,
              amphibious,
              amphibiousLandAttackers,
              new CasualtyList(),
              battleId,
              battlesite,
              allowMultipleHitsPerUnit);
      final List<Unit> killed = editSelection.getKilled();
      // if partial retreat is possible, kill amphibious units first
      if (isPartialAmphibiousRetreat(data)) {
        killAmphibiousFirst(killed, targetsToPickFrom);
      }
      return editSelection;
    }
    if (dice.getHits() == 0) {
      return new CasualtyDetails(List.of(), List.of(), true);
    }
    int hitsRemaining = dice.getHits();
    if (isTransportCasualtiesRestricted(data)) {
      hitsRemaining = extraHits;
    }
    if (!isEditMode && allTargetsOneTypeOneHitPoint(targetsToPickFrom, dependents)) {
      final List<Unit> killed = new ArrayList<>();
      final Iterator<Unit> iter = targetsToPickFrom.iterator();
      for (int i = 0; i < hitsRemaining; i++) {
        if (i >= targetsToPickFrom.size()) {
          break;
        }
        killed.add(iter.next());
      }
      return new CasualtyDetails(killed, List.of(), true);
    }
    // Create production cost map, Maybe should do this elsewhere, but in case prices change, we do
    // it here.
    final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(player, data);
    final Tuple<CasualtyList, List<Unit>> defaultCasualtiesAndSortedTargets =
        getDefaultCasualties(
            targetsToPickFrom,
            hitsRemaining,
            defending,
            player,
            enemyUnits,
            amphibious,
            amphibiousLandAttackers,
            battlesite,
            costs,
            territoryEffects,
            data,
            allowMultipleHitsPerUnit);
    final CasualtyList defaultCasualties = defaultCasualtiesAndSortedTargets.getFirst();
    final List<Unit> sortedTargetsToPickFrom = defaultCasualtiesAndSortedTargets.getSecond();
    if (sortedTargetsToPickFrom.size() != targetsToPickFrom.size()
        || !targetsToPickFrom.containsAll(sortedTargetsToPickFrom)
        || !sortedTargetsToPickFrom.containsAll(targetsToPickFrom)) {
      throw new IllegalStateException(
          "sortedTargetsToPickFrom must contain the same units as targetsToPickFrom list");
    }
    final int totalHitpoints =
        (allowMultipleHitsPerUnit
            ? getTotalHitpointsLeft(sortedTargetsToPickFrom)
            : sortedTargetsToPickFrom.size());
    final CasualtyDetails casualtySelection;
    if (hitsRemaining >= totalHitpoints) {
      casualtySelection = new CasualtyDetails(defaultCasualties, true);
    } else {
      casualtySelection =
          tripleaPlayer.selectCasualties(
              sortedTargetsToPickFrom,
              dependents,
              hitsRemaining,
              text,
              dice,
              player,
              friendlyUnits,
              enemyUnits,
              amphibious,
              amphibiousLandAttackers,
              defaultCasualties,
              battleId,
              battlesite,
              allowMultipleHitsPerUnit);
    }
    final List<Unit> killed = casualtySelection.getKilled();
    // if partial retreat is possible, kill amphibious units first
    if (isPartialAmphibiousRetreat(data)) {
      killAmphibiousFirst(killed, sortedTargetsToPickFrom);
    }
    final List<Unit> damaged = casualtySelection.getDamaged();
    int numhits = killed.size();
    if (!allowMultipleHitsPerUnit) {
      damaged.clear();
    } else {
      for (final Unit unit : killed) {
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        final int damageToUnit = Collections.frequency(damaged, unit);
        // allowed damage
        numhits += Math.max(0, Math.min(damageToUnit, (ua.getHitPoints() - (1 + unit.getHits()))));
        // remove from damaged list, since they will die
        damaged.removeIf(unit::equals);
      }
    }
    // check right number
    if (!isEditMode && numhits + damaged.size() != Math.min(hitsRemaining, totalHitpoints)) {
      tripleaPlayer.reportError("Wrong number of casualties selected");
      if (headLess) {
        log.severe(
            "Possible Infinite Loop: Wrong number of casualties selected: number of hits on units "
                + (numhits + damaged.size())
                + " != number of hits to take "
                + Math.min(hitsRemaining, totalHitpoints)
                + ", for "
                + casualtySelection.toString());
      }
      return selectCasualties(
          player,
          sortedTargetsToPickFrom,
          friendlyUnits,
          enemyUnits,
          amphibious,
          amphibiousLandAttackers,
          battlesite,
          territoryEffects,
          bridge,
          text,
          dice,
          defending,
          battleId,
          headLess,
          extraHits,
          allowMultipleHitsPerUnit);
    }
    // check we have enough of each type
    if (!sortedTargetsToPickFrom.containsAll(killed)
        || !sortedTargetsToPickFrom.containsAll(damaged)) {
      tripleaPlayer.reportError("Cannot remove enough units of those types");
      if (headLess) {
        log.severe(
            "Possible Infinite Loop: Cannot remove enough units of those types: targets "
                + MyFormatter.unitsToTextNoOwner(sortedTargetsToPickFrom)
                + ", for "
                + casualtySelection.toString());
      }
      return selectCasualties(
          player,
          sortedTargetsToPickFrom,
          friendlyUnits,
          enemyUnits,
          amphibious,
          amphibiousLandAttackers,
          battlesite,
          territoryEffects,
          bridge,
          text,
          dice,
          defending,
          battleId,
          headLess,
          extraHits,
          allowMultipleHitsPerUnit);
    }
    return casualtySelection;
  }

  private static void killAmphibiousFirst(final List<Unit> killed, final Collection<Unit> targets) {
    // Get a list of all selected killed units that are NOT amphibious
    final Predicate<Unit> match = Matches.unitIsLand().and(Matches.unitWasNotAmphibious());
    final Collection<Unit> killedNonAmphibUnits =
        new ArrayList<>(CollectionUtils.getMatches(killed, match));
    // If all killed units are amphibious, just return them
    if (killedNonAmphibUnits.isEmpty()) {
      return;
    }
    // Get a list of all units that are amphibious and remove those that are killed
    final Collection<Unit> allAmphibUnits =
        new ArrayList<>(CollectionUtils.getMatches(targets, Matches.unitWasAmphibious()));
    allAmphibUnits.removeAll(CollectionUtils.getMatches(killed, Matches.unitWasAmphibious()));
    // Get a collection of the unit types of the amphib units
    final Collection<UnitType> amphibTypes = new ArrayList<>();
    for (final Unit unit : allAmphibUnits) {
      final UnitType ut = unit.getType();
      if (!amphibTypes.contains(ut)) {
        amphibTypes.add(ut);
      }
    }
    // For each killed unit- see if there is an amphib unit that can be killed instead
    for (final Unit unit : killedNonAmphibUnits) {
      if (amphibTypes.contains(unit.getType())) { // add a unit from the collection
        final List<Unit> oneAmphibUnit =
            CollectionUtils.getNMatches(allAmphibUnits, 1, Matches.unitIsOfType(unit.getType()));
        if (!oneAmphibUnit.isEmpty()) {
          final Unit amphibUnit = oneAmphibUnit.iterator().next();
          killed.remove(unit);
          killed.add(amphibUnit);
          allAmphibUnits.remove(amphibUnit);
        } else { // If there are no more units of that type, remove the type from the collection
          amphibTypes.remove(unit.getType());
        }
      }
    }
  }

  /**
   * A unit with two hitpoints will be listed twice if they will die. The first time they are listed
   * it is as damaged. The second time they are listed, it is dead.
   */
  private static Tuple<CasualtyList, List<Unit>> getDefaultCasualties(
      final Collection<Unit> targetsToPickFrom,
      final int hits,
      final boolean defending,
      final GamePlayer player,
      final Collection<Unit> enemyUnits,
      final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers,
      final Territory battlesite,
      final IntegerMap<UnitType> costs,
      final Collection<TerritoryEffect> territoryEffects,
      final GameData data,
      final boolean allowMultipleHitsPerUnit) {
    final CasualtyList defaultCasualtySelection = new CasualtyList();
    // Sort units by power and cost in ascending order
    final List<Unit> sorted =
        sortUnitsForCasualtiesWithSupport(
            targetsToPickFrom,
            defending,
            player,
            enemyUnits,
            amphibious,
            amphibiousLandAttackers,
            battlesite,
            costs,
            territoryEffects,
            data,
            true);
    // Remove two hit bb's selecting them first for default casualties
    int numSelectedCasualties = 0;
    if (allowMultipleHitsPerUnit) {
      for (final Unit unit : sorted) {
        // Stop if we have already selected as many hits as there are targets
        if (numSelectedCasualties >= hits) {
          return Tuple.of(defaultCasualtySelection, sorted);
        }
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        final int extraHitPoints =
            Math.min((hits - numSelectedCasualties), (ua.getHitPoints() - (1 + unit.getHits())));
        for (int i = 0; i < extraHitPoints; i++) {
          numSelectedCasualties++;
          defaultCasualtySelection.addToDamaged(unit);
        }
      }
    }
    // Select units
    for (final Unit unit : sorted) {
      // Stop if we have already selected as many hits as there are targets
      if (numSelectedCasualties >= hits) {
        return Tuple.of(defaultCasualtySelection, sorted);
      }
      defaultCasualtySelection.addToKilled(unit);
      numSelectedCasualties++;
    }
    return Tuple.of(defaultCasualtySelection, sorted);
  }

  /**
   * The purpose of this is to return a list in the PERFECT order of which units should be selected
   * to die first, And that means that certain units MUST BE INTERLEAVED. This list assumes that you
   * have already taken any extra hit points away from any 2 hitpoint units. Example: You have a 1
   * attack Artillery unit that supports, and a 1 attack infantry unit that can receive support. The
   * best selection of units to die is first to take whichever unit has excess, then cut that down
   * til they are both the same size, then to take 1 artillery followed by 1 infantry, followed by 1
   * artillery, then 1 inf, etc, until everyone is dead. If you just return all infantry followed by
   * all artillery, or the other way around, you will be missing out on some important support
   * provided. (Veqryn)
   */
  private static List<Unit> sortUnitsForCasualtiesWithSupport(
      final Collection<Unit> targetsToPickFrom,
      final boolean defending,
      final GamePlayer player,
      final Collection<Unit> enemyUnits,
      final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers,
      final Territory battlesite,
      final IntegerMap<UnitType> costs,
      final Collection<TerritoryEffect> territoryEffects,
      final GameData data,
      final boolean bonus) {

    // Convert unit lists to unit type lists
    final List<UnitType> targetTypes = new ArrayList<>();
    for (final Unit u : targetsToPickFrom) {
      targetTypes.add(u.getType());
    }
    final List<UnitType> amphibTypes = new ArrayList<>();
    if (amphibiousLandAttackers != null) {
      for (final Unit u : amphibiousLandAttackers) {
        amphibTypes.add(u.getType());
      }
    }
    // Calculate hashes and cache key
    int targetsHashCode = 1;
    for (final UnitType ut : targetTypes) {
      targetsHashCode += ut.hashCode();
    }
    targetsHashCode *= 31;
    int amphibHashCode = 1;
    for (final UnitType ut : amphibTypes) {
      amphibHashCode += ut.hashCode();
    }
    amphibHashCode *= 31;
    String key =
        player.getName()
            + "|"
            + battlesite.getName()
            + "|"
            + defending
            + "|"
            + amphibious
            + "|"
            + targetsHashCode
            + "|"
            + amphibHashCode;
    // Check OOL cache
    final List<UnitType> stored = oolCache.get(key);
    if (stored != null) {
      final List<Unit> result = new ArrayList<>();
      final List<Unit> selectFrom = new ArrayList<>(targetsToPickFrom);
      for (final UnitType ut : stored) {
        for (final Iterator<Unit> it = selectFrom.iterator(); it.hasNext(); ) {
          final Unit u = it.next();
          if (ut.equals(u.getType())) {
            result.add(u);
            it.remove();
          }
        }
      }
      return result;
    }
    // Sort enough units to kill off
    final List<Unit> sortedUnitsList = new ArrayList<>(targetsToPickFrom);
    sortedUnitsList.sort(
        new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false)
            .reversed());
    // Sort units starting with strongest so that support gets added to them first
    final UnitBattleComparator unitComparatorWithoutPrimaryPower =
        new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, true);
    final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
    final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();
    final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRollsMap =
        DiceRoll.getUnitPowerAndRollsForNormalBattles(
            sortedUnitsList,
            new ArrayList<>(enemyUnits),
            sortedUnitsList,
            defending,
            data,
            battlesite,
            territoryEffects,
            amphibious,
            amphibiousLandAttackers,
            unitSupportPowerMap,
            unitSupportRollsMap);
    // Sort units starting with weakest for finding the worst units
    Collections.reverse(sortedUnitsList);
    final List<Unit> sortedWellEnoughUnitsList = new ArrayList<>();
    final Map<Unit, Tuple<Integer, Integer>> originalUnitPowerAndRollsMap =
        new HashMap<>(unitPowerAndRollsMap);
    for (int i = 0; i < sortedUnitsList.size(); ++i) {
      // Loop through all target units to find the best unit to take as casualty
      Unit worstUnit = null;
      int minPower = Integer.MAX_VALUE;
      final Set<UnitType> unitTypes = new HashSet<>();
      for (final Unit u : sortedUnitsList) {
        if (unitTypes.contains(u.getType())) {
          continue;
        }
        unitTypes.add(u.getType());
        // Find unit power
        int power = DiceRoll.getTotalPower(Map.of(u, originalUnitPowerAndRollsMap.get(u)), data);
        // Add any support power that it provides to other units
        final IntegerMap<Unit> unitSupportPowerMapForUnit = unitSupportPowerMap.get(u);
        if (unitSupportPowerMapForUnit != null) {
          for (final Unit supportedUnit : unitSupportPowerMapForUnit.keySet()) {
            Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
            if (strengthAndRolls == null) {
              continue;
            }
            // Remove any rolls provided by this support so they aren't counted twice
            final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(u);
            if (unitSupportRollsMapForUnit != null) {
              strengthAndRolls =
                  Tuple.of(
                      strengthAndRolls.getFirst(),
                      strengthAndRolls.getSecond()
                          - unitSupportRollsMapForUnit.getInt(supportedUnit));
            }
            // If one roll then just add the power
            if (strengthAndRolls.getSecond() == 1) {
              power += unitSupportPowerMapForUnit.getInt(supportedUnit);
              continue;
            }
            // Find supported unit power with support
            final Map<Unit, Tuple<Integer, Integer>> supportedUnitMap = new HashMap<>();
            supportedUnitMap.put(supportedUnit, strengthAndRolls);
            final int powerWithSupport = DiceRoll.getTotalPower(supportedUnitMap, data);
            // Find supported unit power without support
            final int strengthWithoutSupport =
                strengthAndRolls.getFirst() - unitSupportPowerMapForUnit.getInt(supportedUnit);
            final Tuple<Integer, Integer> strengthAndRollsWithoutSupport =
                Tuple.of(strengthWithoutSupport, strengthAndRolls.getSecond());
            supportedUnitMap.put(supportedUnit, strengthAndRollsWithoutSupport);
            final int powerWithoutSupport = DiceRoll.getTotalPower(supportedUnitMap, data);
            // Add the actual power provided by the support
            final int addedPower = powerWithSupport - powerWithoutSupport;
            power += addedPower;
          }
        }
        // Add any power from support rolls that it provides to other units
        final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(u);
        if (unitSupportRollsMapForUnit != null) {
          for (final Unit supportedUnit : unitSupportRollsMapForUnit.keySet()) {
            final Tuple<Integer, Integer> strengthAndRolls =
                unitPowerAndRollsMap.get(supportedUnit);
            if (strengthAndRolls == null) {
              continue;
            }
            // Find supported unit power with support
            final Map<Unit, Tuple<Integer, Integer>> supportedUnitMap = new HashMap<>();
            supportedUnitMap.put(supportedUnit, strengthAndRolls);
            final int powerWithSupport = DiceRoll.getTotalPower(supportedUnitMap, data);
            // Find supported unit power without support
            final int rollsWithoutSupport =
                strengthAndRolls.getSecond() - unitSupportRollsMap.get(u).getInt(supportedUnit);
            final Tuple<Integer, Integer> strengthAndRollsWithoutSupport =
                Tuple.of(strengthAndRolls.getFirst(), rollsWithoutSupport);
            supportedUnitMap.put(supportedUnit, strengthAndRollsWithoutSupport);
            final int powerWithoutSupport = DiceRoll.getTotalPower(supportedUnitMap, data);
            // Add the actual power provided by the support
            final int addedPower = powerWithSupport - powerWithoutSupport;
            power += addedPower;
          }
        }
        // Check if unit has lower power
        if (power < minPower
            || (power == minPower && unitComparatorWithoutPrimaryPower.compare(u, worstUnit) < 0)) {
          worstUnit = u;
          minPower = power;
        }
      }
      // Add worst unit to sorted list, update any units it supported, and remove from other
      // collections
      final IntegerMap<Unit> unitSupportPowerMapForUnit = unitSupportPowerMap.get(worstUnit);
      if (unitSupportPowerMapForUnit != null) {
        for (final Unit supportedUnit : unitSupportPowerMapForUnit.keySet()) {
          final Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
          if (strengthAndRolls == null) {
            continue;
          }
          final int strengthWithoutSupport =
              strengthAndRolls.getFirst() - unitSupportPowerMapForUnit.getInt(supportedUnit);
          final Tuple<Integer, Integer> strengthAndRollsWithoutSupport =
              Tuple.of(strengthWithoutSupport, strengthAndRolls.getSecond());
          unitPowerAndRollsMap.put(supportedUnit, strengthAndRollsWithoutSupport);
          sortedUnitsList.remove(supportedUnit);
          sortedUnitsList.add(0, supportedUnit);
        }
      }
      final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(worstUnit);
      if (unitSupportRollsMapForUnit != null) {
        for (final Unit supportedUnit : unitSupportRollsMapForUnit.keySet()) {
          final Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
          if (strengthAndRolls == null) {
            continue;
          }
          final int rollsWithoutSupport =
              strengthAndRolls.getSecond() - unitSupportRollsMapForUnit.getInt(supportedUnit);
          final Tuple<Integer, Integer> strengthAndRollsWithoutSupport =
              Tuple.of(strengthAndRolls.getFirst(), rollsWithoutSupport);
          unitPowerAndRollsMap.put(supportedUnit, strengthAndRollsWithoutSupport);
          sortedUnitsList.remove(supportedUnit);
          sortedUnitsList.add(0, supportedUnit);
        }
      }
      sortedWellEnoughUnitsList.add(worstUnit);
      sortedUnitsList.remove(worstUnit);
      unitPowerAndRollsMap.remove(worstUnit);
      unitSupportPowerMap.remove(worstUnit);
      unitSupportRollsMap.remove(worstUnit);
    }
    sortedWellEnoughUnitsList.addAll(sortedUnitsList);
    // Cache result and all subsets of the result
    final List<UnitType> unitTypes = new ArrayList<>();
    for (final Unit u : sortedWellEnoughUnitsList) {
      unitTypes.add(u.getType());
    }
    for (final Iterator<UnitType> it = unitTypes.iterator(); it.hasNext(); ) {
      oolCache.put(key, new ArrayList<>(unitTypes));
      final UnitType unitTypeToRemove = it.next();
      targetTypes.remove(unitTypeToRemove);
      if (Collections.frequency(targetTypes, unitTypeToRemove)
          < Collections.frequency(amphibTypes, unitTypeToRemove)) {
        amphibTypes.remove(unitTypeToRemove);
      }
      targetsHashCode = 1;
      for (final UnitType ut : targetTypes) {
        targetsHashCode += ut.hashCode();
      }
      targetsHashCode *= 31;
      amphibHashCode = 1;
      for (final UnitType ut : amphibTypes) {
        amphibHashCode += ut.hashCode();
      }
      amphibHashCode *= 31;
      key =
          player.getName()
              + "|"
              + battlesite.getName()
              + "|"
              + defending
              + "|"
              + amphibious
              + "|"
              + targetsHashCode
              + "|"
              + amphibHashCode;
      it.remove();
    }
    return sortedWellEnoughUnitsList;
  }

  public static Map<Unit, Collection<Unit>> getDependents(final Collection<Unit> targets) {
    // just worry about transports
    final Map<Unit, Collection<Unit>> dependents = new HashMap<>();
    for (final Unit target : targets) {
      dependents.put(target, TransportTracker.transportingAndUnloaded(target));
    }
    return dependents;
  }

  /**
   * Checks if the given collections target are all of one category as defined by
   * UnitSeparator.categorize and they are not two hit units.
   *
   * @param targets a collection of target units
   * @param dependents map of depend units for target units
   */
  private static boolean allTargetsOneTypeOneHitPoint(
      final Collection<Unit> targets, final Map<Unit, Collection<Unit>> dependents) {
    final Set<UnitCategory> categorized =
        UnitSeparator.categorize(targets, dependents, false, false);
    if (categorized.size() == 1) {
      final UnitCategory unitCategory = categorized.iterator().next();
      return unitCategory.getHitPoints() - unitCategory.getDamaged() <= 1;
    }
    return false;
  }

  /** Indicates transports can be used as cannon fodder. */
  private static boolean isTransportCasualtiesRestricted(final GameData data) {
    return Properties.getTransportCasualtiesRestricted(data);
  }

  /** Indicates AA casualties are randomly assigned. */
  private static boolean isRandomAaCasualties(final GameData data) {
    return Properties.getRandomAaCasualties(data);
  }

  /** Indicates AA is rolled against each aircraft. */
  private static boolean isRollAaIndividually(final GameData data) {
    return Properties.getRollAaIndividually(data);
  }

  /** Indicates attacker selects AA casualties. */
  private static boolean isChooseAa(final GameData data) {
    return Properties.getChooseAaCasualties(data);
  }

  /** Indicates the attacker can retreat non-amphibious units. */
  private static boolean isPartialAmphibiousRetreat(final GameData data) {
    return Properties.getPartialAmphibiousRetreat(data);
  }
}
