package games.strategy.triplea.delegate;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.net.GUID;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleA;
import games.strategy.triplea.ai.weakAI.WeakAi;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.LinkedIntegerMap;
import games.strategy.util.Triple;
import games.strategy.util.Tuple;

/**
 * Utiltity class for determing casualties and selecting casualties. The code
 * was being dduplicated all over the place.
 */
public class BattleCalculator {
  private static final Map<String, List<UnitType>> oolCache = new ConcurrentHashMap<>();

  public static void clearOolCache() {
    oolCache.clear();
  }

  // There is a problem with this variable, that it isn't
  // private static IntegerMap<UnitType> costsForTuvForAllPlayersMergedAndAveraged;
  // being cleared out when we switch maps.
  // we want to sort in a determined way so that those looking at the dice results
  // can tell what dice is for who
  // we also want to sort by movement, so casualties will be choosen as the
  // units with least movement
  static void sortPreBattle(final List<Unit> units) {
    Collections.sort(units,
        Comparator.comparing(Unit::getType,
            Comparator.comparing(UnitType::getName))
            .thenComparing(UnitComparator.getLowestToHighestMovementComparator()));
  }

  public static int getTotalHitpointsLeft(final Collection<Unit> units) {
    if ((units == null) || units.isEmpty()) {
      return 0;
    }
    int totalHitPoints = 0;
    for (final Unit u : units) {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      totalHitPoints += ua.getHitPoints();
      totalHitPoints -= u.getHits();
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

  /**
   * Choose plane casualties according to specified rules.
   */
  static CasualtyDetails getAaCasualties(final boolean defending, final Collection<Unit> planes,
      final Collection<Unit> allFriendlyUnits, final Collection<Unit> defendingAa, final Collection<Unit> allEnemyUnits,
      final DiceRoll dice, final IDelegateBridge bridge, final PlayerID firingPlayer, final PlayerID hitPlayer,
      final GUID battleId, final Territory terr, final Collection<TerritoryEffect> territoryEffects,
      final boolean amphibious, final Collection<Unit> amphibiousLandAttackers) {
    if (planes.isEmpty()) {
      return new CasualtyDetails();
    }
    final GameData data = bridge.getData();
    final boolean allowMultipleHitsPerUnit =
        !defendingAa.isEmpty()
            && defendingAa.stream().allMatch(Matches.unitAaShotDamageableInsteadOfKillingInstantly());
    if (isChooseAa(data)) {
      final String text = "Select " + dice.getHits() + " casualties from aa fire in " + terr.getName();
      return selectCasualties(null, hitPlayer, planes, allFriendlyUnits, firingPlayer, allEnemyUnits, amphibious,
          amphibiousLandAttackers, terr, territoryEffects, bridge, text, dice, defending, battleId, false,
          dice.getHits(), allowMultipleHitsPerUnit);
    }

    if (Properties.getLowLuck(data) || Properties.getLowLuckAaOnly(data)) {
      return getLowLuckAaCasualties(defending, planes, defendingAa, dice, bridge, allowMultipleHitsPerUnit);
    }

    // priority goes: choose -> individually -> random
    // if none are set, we roll individually
    if (isRollAaIndividually(data)) {
      return individuallyFiredAaCasualties(defending, planes, defendingAa, dice, bridge, allowMultipleHitsPerUnit);
    }
    if (isRandomAaCasualties(data)) {
      return randomAaCasualties(planes, dice, bridge, allowMultipleHitsPerUnit);
    }
    return individuallyFiredAaCasualties(defending, planes, defendingAa, dice, bridge, allowMultipleHitsPerUnit);
  }

  /**
   * http://triplea.sourceforge.net/mywiki/Forum#nabble-td4658925%7Ca4658925
   * returns two lists, the first list is the air units that can be evenly divided into groups of 3 or 6 (depending on
   * radar)
   * the second list is all the air units that do not fit in the first list
   */
  private static Tuple<List<List<Unit>>, List<Unit>> categorizeLowLuckAirUnits(final Collection<Unit> units,
      final int groupSize) {
    final Collection<UnitCategory> categorizedAir = UnitSeperator.categorize(units, null, false, true);
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

  private static CasualtyDetails getLowLuckAaCasualties(final boolean defending, final Collection<Unit> planes,
      final Collection<Unit> defendingAa, final DiceRoll dice, final IDelegateBridge bridge,
      final boolean allowMultipleHitsPerUnit) {
    {
      final Set<Unit> duplicatesCheckSet1 = new HashSet<>(planes);
      if (planes.size() != duplicatesCheckSet1.size()) {
        throw new IllegalStateException(
            "Duplicate Units Detected: Original List:" + planes + "  HashSet:" + duplicatesCheckSet1);
      }
      final Set<Unit> duplicatesCheckSet2 = new HashSet<>(defendingAa);
      if (defendingAa.size() != duplicatesCheckSet2.size()) {
        throw new IllegalStateException(
            "Duplicate Units Detected: Original List:" + defendingAa + "  HashSet:" + duplicatesCheckSet2);
      }
    }
    int hitsLeft = dice.getHits();
    if (hitsLeft <= 0) {
      return new CasualtyDetails();
    }
    // if we can damage units, do it now
    final CasualtyDetails finalCasualtyDetails = new CasualtyDetails();
    final GameData data = bridge.getData();
    final Tuple<Integer, Integer> attackThenDiceSides =
        DiceRoll.getAAattackAndMaxDiceSides(defendingAa, data, !defending);
    final int highestAttack = attackThenDiceSides.getFirst();
    if (highestAttack < 1) {
      return new CasualtyDetails();
    }
    final int chosenDiceSize = attackThenDiceSides.getSecond();
    final Triple<Integer, Integer, Boolean> triple =
        DiceRoll.getTotalAaPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(null, null, !defending, defendingAa,
            planes, data, false);
    final boolean allSameAttackPower = triple.getThird();
    // multiple HP units need to be counted multiple times:
    final List<Unit> planesList = new ArrayList<>();
    for (final Unit plane : planes) {
      final int hpLeft =
          allowMultipleHitsPerUnit ? (UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits())
              : (Math.min(1, UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits()));
      for (int hp = 0; hp < hpLeft; ++hp) {
        // if allowMultipleHitsPerUnit, then because the number of rolls exactly equals the hitpoints of all units,
        // we roll multiple times for any unit with multiple hitpoints
        planesList.add(plane);
      }
    }
    // killing the air by groups does not work if the the attack power is different for some of the rolls
    // also, killing by groups does not work if some of the aa guns have 'MayOverStackAA' and we have more hits than the
    // total number of
    // groups (including the remainder group)
    // (when i mean, 'does not work', i mean that it is no longer a mathematically fair way to find casualties)
    // find group size (if no groups, do dice sides)
    final int groupSize;
    if (allSameAttackPower) {
      groupSize = chosenDiceSize / highestAttack;
    } else {
      groupSize = chosenDiceSize;
    }
    final int numberOfGroupsByDiceSides = (int) Math.ceil((double) planesList.size() / (double) groupSize);
    final boolean tooManyHitsToDoGroups = hitsLeft > numberOfGroupsByDiceSides;
    if (!allSameAttackPower || tooManyHitsToDoGroups || ((chosenDiceSize % highestAttack) != 0)) {
      // we have too many hits, so just pick randomly
      return randomAaCasualties(planes, dice, bridge, allowMultipleHitsPerUnit);
    }

    // if we have a group of 6 fighters and 2 bombers, and dicesides is 6, and attack was 1, then we would want 1
    // fighter to die for sure.
    // this is what groupsize is for.
    // if the attack is greater than 1 though, and all use the same attack power, then the group size can be smaller
    // (ie: attack is 2, and
    // we have 3 fighters and 2 bombers, we would want 1 fighter to die for sure).
    // categorize with groupSize
    final Tuple<List<List<Unit>>, List<Unit>> airSplit = categorizeLowLuckAirUnits(planesList, groupSize);
    // the non rolling air units
    // if we are less hits than the number of groups, OR we have equal hits to number of groups but we also have a
    // remainder that is equal
    // to or greater than group size,
    // THEN we need to make sure to pick randomly, and include the remainder group. (reason we do not do this with any
    // remainder size, is
    // because we might have missed the dice roll to hit the remainder)
    if (hitsLeft < (airSplit.getFirst().size()
        + ((int) Math.ceil((double) airSplit.getSecond().size() / (double) groupSize)))) {
      // fewer hits than groups.
      final List<Unit> tempPossibleHitUnits = new ArrayList<>();
      for (final List<Unit> group : airSplit.getFirst()) {
        tempPossibleHitUnits.add(group.get(0));
      }
      if (airSplit.getSecond().size() > 0) {
        // if we have a remainder group, we need to add some of them into the mix
        // but we have to do so randomly.
        final List<Unit> remainders = new ArrayList<>(airSplit.getSecond());
        if (remainders.size() == 1) {
          tempPossibleHitUnits.add(remainders.remove(0));
        } else {
          final int numberOfRemainderGroups = (int) Math.ceil((double) remainders.size() / (double) groupSize);
          final int[] randomRemainder = bridge.getRandom(remainders.size(), numberOfRemainderGroups, null,
              DiceType.ENGINE, "Deciding which planes should die due to AA fire");
          int pos2 = 0;
          for (final int element : randomRemainder) {
            pos2 += element;
            tempPossibleHitUnits.add(remainders.remove(pos2 % remainders.size()));
          }
        }
      }
      final int[] hitRandom = bridge.getRandom(tempPossibleHitUnits.size(), hitsLeft, null, DiceType.ENGINE,
          "Deciding which planes should die due to AA fire");
      // now we find the
      int pos = 0;
      for (final int element : hitRandom) {
        pos += element;
        final Unit unitHit = tempPossibleHitUnits.remove(pos % tempPossibleHitUnits.size());
        if (allowMultipleHitsPerUnit && (Collections.frequency(finalCasualtyDetails.getDamaged(),
            unitHit) < (getTotalHitpointsLeft(unitHit) - 1))) {
          finalCasualtyDetails.addToDamaged(unitHit);
        } else {
          finalCasualtyDetails.addToKilled(unitHit);
        }
      }
    } else {
      // kill one in every group
      for (final List<Unit> group : airSplit.getFirst()) {
        final Unit unitHit = group.get(0);
        if (allowMultipleHitsPerUnit && (Collections.frequency(finalCasualtyDetails.getDamaged(),
            unitHit) < (getTotalHitpointsLeft(unitHit) - 1))) {
          finalCasualtyDetails.addToDamaged(unitHit);
        } else {
          finalCasualtyDetails.addToKilled(unitHit);
        }
        hitsLeft--;
      }
      // for any hits left over...
      if (hitsLeft == airSplit.getSecond().size()) {
        for (final Unit unitHit : airSplit.getSecond()) {
          if (allowMultipleHitsPerUnit && (Collections.frequency(finalCasualtyDetails.getDamaged(),
              unitHit) < (getTotalHitpointsLeft(unitHit) - 1))) {
            finalCasualtyDetails.addToDamaged(unitHit);
          } else {
            finalCasualtyDetails.addToKilled(unitHit);
          }
        }
      } else if (hitsLeft != 0) {
        // the remainder
        // roll all at once to prevent frequent random calls, important for pbem games
        final int[] hitRandom = bridge.getRandom(airSplit.getSecond().size(), hitsLeft, null, DiceType.ENGINE,
            "Deciding which planes should die due to AA fire");
        int pos = 0;
        for (final int element : hitRandom) {
          pos += element;
          final Unit unitHit = airSplit.getSecond().remove(pos % airSplit.getSecond().size());
          if (allowMultipleHitsPerUnit && (Collections.frequency(finalCasualtyDetails.getDamaged(),
              unitHit) < (getTotalHitpointsLeft(unitHit) - 1))) {
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
          "wrong number of casulaties, expected:" + dice + " but got: " + finalCasualtyDetails);
    }
    return finalCasualtyDetails;
  }

  /**
   * Choose plane casualties randomly.
   */
  public static CasualtyDetails randomAaCasualties(final Collection<Unit> planes, final DiceRoll dice,
      final IDelegateBridge bridge, final boolean allowMultipleHitsPerUnit) {
    {
      final Set<Unit> duplicatesCheckSet1 = new HashSet<>(planes);
      if (planes.size() != duplicatesCheckSet1.size()) {
        throw new IllegalStateException(
            "Duplicate Units Detected: Original List:" + planes + "  HashSet:" + duplicatesCheckSet1);
      }
    }
    final int hitsLeft = dice.getHits();
    if (hitsLeft <= 0) {
      return new CasualtyDetails();
    }
    final CasualtyDetails finalCasualtyDetails = new CasualtyDetails();
    // normal behavior is instant kill, which means planes.size()
    final int planeHitPoints = (allowMultipleHitsPerUnit ? getTotalHitpointsLeft(planes) : planes.size());
    final List<Unit> planesList = new ArrayList<>();
    for (final Unit plane : planes) {
      final int hpLeft =
          allowMultipleHitsPerUnit ? (UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits())
              : (Math.min(1, UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits()));
      for (int hp = 0; hp < hpLeft; ++hp) {
        // if allowMultipleHitsPerUnit, then because the number of rolls exactly equals the hitpoints of all units,
        // we roll multiple times for any unit with multiple hitpoints
        planesList.add(plane);
      }
    }
    // We need to choose which planes die randomly
    if (hitsLeft < planeHitPoints) {
      // roll all at once to prevent frequent random calls, important for pbem games
      final int[] hitRandom = bridge.getRandom(planeHitPoints, hitsLeft, null, DiceType.ENGINE,
          "Deciding which planes should die due to AA fire");
      int pos = 0;
      for (final int element : hitRandom) {
        pos += element;
        final Unit unitHit = planesList.remove(pos % planesList.size());
        if (allowMultipleHitsPerUnit && (Collections.frequency(finalCasualtyDetails.getDamaged(),
            unitHit) < (getTotalHitpointsLeft(unitHit) - 1))) {
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
   * Choose plane casualties based on individual AA shots at each aircraft.
   */
  private static CasualtyDetails individuallyFiredAaCasualties(final boolean defending, final Collection<Unit> planes,
      final Collection<Unit> defendingAa, final DiceRoll dice, final IDelegateBridge bridge,
      final boolean allowMultipleHitsPerUnit) {
    // if we have aa guns that are not infinite, then we need to randomly decide the aa casualties since there are not
    // enough rolls to have
    // a single roll for each aircraft, or too many rolls
    // normal behavior is instant kill, which means planes.size()
    final int planeHitPoints = (allowMultipleHitsPerUnit ? getTotalHitpointsLeft(planes) : planes.size());

    if (DiceRoll.getTotalAAattacks(defendingAa, planes) != planeHitPoints) {
      return randomAaCasualties(planes, dice, bridge, allowMultipleHitsPerUnit);
    }
    final Triple<Integer, Integer, Boolean> triple =
        DiceRoll.getTotalAaPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(null, null, !defending, defendingAa,
            planes, bridge.getData(), false);
    final boolean allSameAttackPower = triple.getThird();
    if (!allSameAttackPower) {
      return randomAaCasualties(planes, dice, bridge, allowMultipleHitsPerUnit);
    }
    final Tuple<Integer, Integer> attackThenDiceSides =
        DiceRoll.getAAattackAndMaxDiceSides(defendingAa, bridge.getData(), !defending);
    final int highestAttack = attackThenDiceSides.getFirst();
    // int chosenDiceSize = attackThenDiceSides[1];
    final CasualtyDetails finalCasualtyDetails = new CasualtyDetails();
    final int hits = dice.getHits();
    final List<Unit> planesList = new ArrayList<>();
    for (final Unit plane : planes) {
      final int hpLeft =
          allowMultipleHitsPerUnit ? (UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits())
              : (Math.min(1, UnitAttachment.get(plane.getType()).getHitPoints() - plane.getHits()));
      for (int hp = 0; hp < hpLeft; ++hp) {
        // if allowMultipleHitsPerUnit, then because the number of rolls exactly equals the hitpoints of all units,
        // we roll multiple times for any unit with multiple hitpoints
        planesList.add(plane);
      }
    }
    // We need to choose which planes die based on their position in the list and the individual AA rolls
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
              && (Collections.frequency(finalCasualtyDetails.getDamaged(), unit) < (getTotalHitpointsLeft(unit) - 1))) {
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
   * @param battleId
   *        may be null if we are not in a battle (eg, if this is an aa fire due to moving).
   */
  public static CasualtyDetails selectCasualties(final String step, final PlayerID player,
      final Collection<Unit> targetsToPickFrom, final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer,
      final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
      final Territory battlesite, final Collection<TerritoryEffect> territoryEffects, final IDelegateBridge bridge,
      final String text, final DiceRoll dice, final boolean defending, final GUID battleId, final boolean headLess,
      final int extraHits, final boolean allowMultipleHitsPerUnit) {
    if (targetsToPickFrom.isEmpty()) {
      return new CasualtyDetails();
    }
    if (!friendlyUnits.containsAll(targetsToPickFrom)) {
      throw new IllegalStateException("friendlyUnits should but does not contain all units from targetsToPickFrom");
    }
    final GameData data = bridge.getData();
    final boolean isEditMode = BaseEditDelegate.getEditMode(data);
    final ITripleAPlayer tripleaPlayer = player.isNull()
        ? new WeakAi(player.getName(), TripleA.WEAK_COMPUTER_PLAYER_TYPE)
        : (ITripleAPlayer) bridge.getRemotePlayer(player);
    final Map<Unit, Collection<Unit>> dependents = headLess ? Collections.emptyMap() : getDependents(targetsToPickFrom);
    if (isEditMode && !headLess) {
      final CasualtyDetails editSelection = tripleaPlayer.selectCasualties(targetsToPickFrom, dependents, 0, text, dice,
          player, friendlyUnits, enemyPlayer, enemyUnits, amphibious, amphibiousLandAttackers, new CasualtyList(),
          battleId, battlesite, allowMultipleHitsPerUnit);
      final List<Unit> killed = editSelection.getKilled();
      // if partial retreat is possible, kill amphibious units first
      if (isPartialAmphibiousRetreat(data)) {
        killAmphibiousFirst(killed, targetsToPickFrom);
      }
      return editSelection;
    }
    if (dice.getHits() == 0) {
      return new CasualtyDetails(Collections.emptyList(), Collections.emptyList(), true);
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
      return new CasualtyDetails(killed, Collections.emptyList(), true);
    }
    // Create production cost map, Maybe should do this elsewhere, but in case prices change, we do it here.
    final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(player, data);
    final Tuple<CasualtyList, List<Unit>> defaultCasualtiesAndSortedTargets = getDefaultCasualties(targetsToPickFrom,
        hitsRemaining, defending, player, enemyUnits, amphibious, amphibiousLandAttackers,
        battlesite, costs, territoryEffects, data, allowMultipleHitsPerUnit, true);
    final CasualtyList defaultCasualties = defaultCasualtiesAndSortedTargets.getFirst();
    final List<Unit> sortedTargetsToPickFrom = defaultCasualtiesAndSortedTargets.getSecond();
    if ((sortedTargetsToPickFrom.size() != targetsToPickFrom.size())
        || !targetsToPickFrom.containsAll(sortedTargetsToPickFrom)
        || !sortedTargetsToPickFrom.containsAll(targetsToPickFrom)) {
      throw new IllegalStateException("sortedTargetsToPickFrom must contain the same units as targetsToPickFrom list");
    }
    final int totalHitpoints =
        (allowMultipleHitsPerUnit ? getTotalHitpointsLeft(sortedTargetsToPickFrom) : sortedTargetsToPickFrom.size());
    final CasualtyDetails casualtySelection;
    if (hitsRemaining >= totalHitpoints) {
      casualtySelection = new CasualtyDetails(defaultCasualties, true);
    } else {
      casualtySelection = tripleaPlayer.selectCasualties(sortedTargetsToPickFrom, dependents, hitsRemaining, text, dice,
          player, friendlyUnits, enemyPlayer, enemyUnits, amphibious, amphibiousLandAttackers, defaultCasualties,
          battleId, battlesite, allowMultipleHitsPerUnit);
    }
    List<Unit> killed = casualtySelection.getKilled();
    // if partial retreat is possible, kill amphibious units first
    if (isPartialAmphibiousRetreat(data)) {
      killed = killAmphibiousFirst(killed, sortedTargetsToPickFrom);
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
        final Iterator<Unit> iter = damaged.iterator();
        while (iter.hasNext()) {
          if (unit.equals(iter.next())) {
            // remove from damaged list, since they will die
            iter.remove();
          }
        }
      }
    }
    // check right number
    if (!isEditMode
        && !((numhits + damaged.size()) == ((hitsRemaining > totalHitpoints) ? totalHitpoints : hitsRemaining))) {
      tripleaPlayer.reportError("Wrong number of casualties selected");
      if (headLess) {
        System.err.println("Possible Infinite Loop: Wrong number of casualties selected: number of hits on units "
            + (numhits + damaged.size()) + " != number of hits to take "
            + (hitsRemaining > totalHitpoints ? totalHitpoints : hitsRemaining) + ", for "
            + casualtySelection.toString());
      }
      return selectCasualties(step, player, sortedTargetsToPickFrom, friendlyUnits, enemyPlayer, enemyUnits, amphibious,
          amphibiousLandAttackers, battlesite, territoryEffects, bridge, text, dice, defending, battleId, headLess,
          extraHits, allowMultipleHitsPerUnit);
    }
    // check we have enough of each type
    if (!sortedTargetsToPickFrom.containsAll(killed) || !sortedTargetsToPickFrom.containsAll(damaged)) {
      tripleaPlayer.reportError("Cannot remove enough units of those types");
      if (headLess) {
        System.err.println("Possible Infinite Loop: Cannot remove enough units of those types: targets "
            + MyFormatter.unitsToTextNoOwner(sortedTargetsToPickFrom) + ", for " + casualtySelection.toString());
      }
      return selectCasualties(step, player, sortedTargetsToPickFrom, friendlyUnits, enemyPlayer, enemyUnits, amphibious,
          amphibiousLandAttackers, battlesite, territoryEffects, bridge, text, dice, defending, battleId, headLess,
          extraHits, allowMultipleHitsPerUnit);
    }
    return casualtySelection;
  }

  private static List<Unit> killAmphibiousFirst(final List<Unit> killed, final Collection<Unit> targets) {
    final Collection<Unit> killedNonAmphibUnits = new ArrayList<>();
    // Get a list of all selected killed units that are NOT amphibious
    final Predicate<Unit> match = Matches.unitIsLand().and(Matches.unitWasNotAmphibious());
    killedNonAmphibUnits.addAll(CollectionUtils.getMatches(killed, match));
    // If all killed units are amphibious, just return them
    if (killedNonAmphibUnits.isEmpty()) {
      return killed;
    }
    // Get a list of all units that are amphibious and remove those that are killed
    final Collection<Unit> allAmphibUnits = new ArrayList<>();
    allAmphibUnits.addAll(CollectionUtils.getMatches(targets, Matches.unitWasAmphibious()));
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
        if (oneAmphibUnit.size() > 0) {
          final Unit amphibUnit = oneAmphibUnit.iterator().next();
          killed.remove(unit);
          killed.add(amphibUnit);
          allAmphibUnits.remove(amphibUnit);
        } else { // If there are no more units of that type, remove the type from the collection
          amphibTypes.remove(unit.getType());
        }
      }
    }
    return killed;
  }

  /**
   * A unit with two hitpoints will be listed twice if they will die. The first time they are listed it is as damaged.
   * The second time they
   * are listed, it is dead.
   */
  private static Tuple<CasualtyList, List<Unit>> getDefaultCasualties(final Collection<Unit> targetsToPickFrom,
      final int hits, final boolean defending, final PlayerID player, final Collection<Unit> enemyUnits,
      final boolean amphibious, final Collection<Unit> amphibiousLandAttackers, final Territory battlesite,
      final IntegerMap<UnitType> costs, final Collection<TerritoryEffect> territoryEffects, final GameData data,
      final boolean allowMultipleHitsPerUnit, final boolean bonus) {
    final CasualtyList defaultCasualtySelection = new CasualtyList();
    // Sort units by power and cost in ascending order
    final List<Unit> sorted = sortUnitsForCasualtiesWithSupport(targetsToPickFrom, defending, player,
        enemyUnits, amphibious, amphibiousLandAttackers, battlesite, costs,
        territoryEffects, data, bonus);
    // Remove two hit bb's selecting them first for default casualties
    int numSelectedCasualties = 0;
    if (allowMultipleHitsPerUnit) {
      for (final Unit unit : sorted) {
        // Stop if we have already selected as many hits as there are targets
        if (numSelectedCasualties >= hits) {
          return Tuple.of(defaultCasualtySelection, sorted);
        }
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        final int extraHitPoints = Math.min((hits - numSelectedCasualties), (ua.getHitPoints() - (1 + unit.getHits())));
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
   * The purpose of this is to return a list in the PERFECT order of which units should be selected to die first,
   * And that means that certain units MUST BE INTERLEAVED.
   * This list assumes that you have already taken any extra hit points away from any 2 hitpoint units.
   * Example: You have a 1 attack Artillery unit that supports, and a 1 attack infantry unit that can receive support.
   * The best selection of units to die is first to take whichever unit has excess, then cut that down til they are both
   * the same size,
   * then to take 1 artillery followed by 1 infantry, followed by 1 artillery, then 1 inf, etc, until everyone is dead.
   * If you just return all infantry followed by all artillery, or the other way around, you will be missing out on some
   * important support
   * provided.
   * (Veqryn)
   */
  private static List<Unit> sortUnitsForCasualtiesWithSupport(final Collection<Unit> targetsToPickFrom,
      final boolean defending, final PlayerID player, final Collection<Unit> enemyUnits, final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers, final Territory battlesite, final IntegerMap<UnitType> costs,
      final Collection<TerritoryEffect> territoryEffects, final GameData data, final boolean bonus) {

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
    String key = player.getName() + "|" + battlesite.getName() + "|" + defending + "|" + amphibious + "|"
        + targetsHashCode + "|" + amphibHashCode;
    // Check OOL cache
    final List<UnitType> stored = oolCache.get(key);
    if (stored != null) {
      // System.out.println("Hit with cacheSize=" + oolCache.size() + ", key=" + key);
      final List<Unit> result = new ArrayList<>();
      final List<Unit> selectFrom = new ArrayList<>(targetsToPickFrom);
      for (final UnitType ut : stored) {
        for (final Iterator<Unit> it = selectFrom.iterator(); it.hasNext();) {
          final Unit u = it.next();
          if (ut.equals(u.getType())) {
            result.add(u);
            it.remove();
          }
        }
      }
      return result;
    }
    // System.out.println("Miss with cacheSize=" + oolCache.size() + ", key=" + key);
    // Sort enough units to kill off
    final List<Unit> sortedUnitsList = new ArrayList<>(targetsToPickFrom);
    Collections.sort(sortedUnitsList, new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false));
    // Sort units starting with strongest so that support gets added to them first
    Collections.reverse(sortedUnitsList);
    final UnitBattleComparator unitComparatorWithoutPrimaryPower =
        new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, true);
    final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
    final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();
    final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRollsMap = DiceRoll.getUnitPowerAndRollsForNormalBattles(
        sortedUnitsList, new ArrayList<>(enemyUnits), defending, false, data, battlesite,
        territoryEffects, amphibious, amphibiousLandAttackers, unitSupportPowerMap, unitSupportRollsMap);
    // Sort units starting with weakest for finding the worst units
    Collections.reverse(sortedUnitsList);
    final List<Unit> sortedWellEnoughUnitsList = new ArrayList<>();
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
        final Map<Unit, Tuple<Integer, Integer>> currentUnitMap = new HashMap<>();
        currentUnitMap.put(u, unitPowerAndRollsMap.get(u));
        int power = DiceRoll.getTotalPower(currentUnitMap, data);
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
              strengthAndRolls = Tuple.of(strengthAndRolls.getFirst(),
                  strengthAndRolls.getSecond() - unitSupportRollsMapForUnit.getInt(supportedUnit));
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
            final Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
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
        if ((power < minPower) || ((power == minPower) && (unitComparatorWithoutPrimaryPower.compare(u, worstUnit)
            < 0))) {
          worstUnit = u;
          minPower = power;
        }
      }
      // Add worst unit to sorted list, update any units it supported, and remove from other collections
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
    for (final Iterator<UnitType> it = unitTypes.iterator(); it.hasNext();) {
      oolCache.put(key, new ArrayList<>(unitTypes));
      final UnitType unitTypeToRemove = it.next();
      targetTypes.remove(unitTypeToRemove);
      if (Collections.frequency(targetTypes, unitTypeToRemove) < Collections.frequency(amphibTypes, unitTypeToRemove)) {
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
      key = player.getName() + "|" + battlesite.getName() + "|" + defending + "|" + amphibious + "|" + targetsHashCode
          + "|" + amphibHashCode;
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
   * Checks if the given collections target are all of one category as defined
   * by UnitSeperator.categorize and they are not two hit units.
   *
   * @param targets
   *        a collection of target units
   * @param dependents
   *        map of depend units for target units
   */
  private static boolean allTargetsOneTypeOneHitPoint(final Collection<Unit> targets,
      final Map<Unit, Collection<Unit>> dependents) {
    final Set<UnitCategory> categorized = UnitSeperator.categorize(targets, dependents, false, false);
    if (categorized.size() == 1) {
      final UnitCategory unitCategory = categorized.iterator().next();
      return (unitCategory.getHitPoints() - unitCategory.getDamaged()) <= 1;
    }
    return false;
  }

  private static int getRolls(final Collection<Unit> units, final PlayerID id,
      final boolean defend, final boolean bombing, final Set<List<UnitSupportAttachment>> supportRulesFriendly,
      final IntegerMap<UnitSupportAttachment> supportLeftFriendlyCopy,
      final Set<List<UnitSupportAttachment>> supportRulesEnemy,
      final IntegerMap<UnitSupportAttachment> supportLeftEnemyCopy,
      final Collection<TerritoryEffect> territoryEffects) {
    int count = 0;
    for (final Unit unit : units) {
      final int unitRoll = getRolls(unit, id, defend, bombing, supportRulesFriendly, supportLeftFriendlyCopy,
          supportRulesEnemy, supportLeftEnemyCopy, territoryEffects);
      count += unitRoll;
    }
    return count;
  }

  static int getRolls(final Collection<Unit> units, final PlayerID id, final boolean defend,
      final boolean bombing, final Collection<TerritoryEffect> territoryEffects) {
    return getRolls(units, id, defend, bombing, new HashSet<>(),
        new IntegerMap<>(), new HashSet<>(),
        new IntegerMap<>(), territoryEffects);
  }

  private static int getRolls(final Unit unit, final PlayerID id, final boolean defend,
      final boolean bombing, final Set<List<UnitSupportAttachment>> supportRulesFriendly,
      final IntegerMap<UnitSupportAttachment> supportLeftFriendlyCopy,
      final Set<List<UnitSupportAttachment>> supportRulesEnemy,
      final IntegerMap<UnitSupportAttachment> supportLeftEnemyCopy,
      final Collection<TerritoryEffect> territoryEffects) {
    final UnitAttachment unitAttachment = UnitAttachment.get(unit.getType());
    int rolls;
    if (defend) {
      rolls = unitAttachment.getDefenseRolls(id);
    } else {
      rolls = unitAttachment.getAttackRolls(id);
    }
    final Map<UnitSupportAttachment, LinkedIntegerMap<Unit>> dummyEmptyMap =
        new HashMap<>();
    rolls += DiceRoll.getSupport(unit, supportRulesFriendly, supportLeftFriendlyCopy, dummyEmptyMap, null, false, true);
    rolls += DiceRoll.getSupport(unit, supportRulesEnemy, supportLeftEnemyCopy, dummyEmptyMap, null, false, true);
    rolls = Math.max(0, rolls);
    // if we are strategic bombing, we do not care what the strength of the unit is...
    if (bombing) {
      return rolls;
    }
    int strength;
    if (defend) {
      strength = unitAttachment.getDefense(unit.getOwner());
    } else {
      strength = unitAttachment.getAttack(unit.getOwner());
    }
    // TODO: we should add in isMarine bonus too...
    strength +=
        DiceRoll.getSupport(unit, supportRulesFriendly, supportLeftFriendlyCopy, dummyEmptyMap, null, true, false);
    strength += DiceRoll.getSupport(unit, supportRulesEnemy, supportLeftEnemyCopy, dummyEmptyMap, null, true, false);
    strength += TerritoryEffectHelper.getTerritoryCombatBonus(unit.getType(), territoryEffects, defend);
    if (strength <= 0) {
      rolls = 0;
    }
    return rolls;
  }

  static int getRolls(final Unit unit, final PlayerID id, final boolean defend,
      final boolean bombing, final Collection<TerritoryEffect> territoryEffects) {
    return getRolls(unit, id, defend, bombing, new HashSet<>(),
        new IntegerMap<>(), new HashSet<>(),
        new IntegerMap<>(), territoryEffects);
  }

  /**
   * @return Can transports be used as cannon fodder.
   */
  private static boolean isTransportCasualtiesRestricted(final GameData data) {
    return Properties.getTransportCasualtiesRestricted(data);
  }

  /**
   * @return Random AA Casualties - casualties randomly assigned.
   */
  private static boolean isRandomAaCasualties(final GameData data) {
    return Properties.getRandomAaCasualties(data);
  }

  /**
   * @return Roll AA Individually - roll against each aircraft.
   */
  private static boolean isRollAaIndividually(final GameData data) {
    return Properties.getRollAaIndividually(data);
  }

  /**
   * @return Choose AA - attacker selects casualties.
   */
  private static boolean isChooseAa(final GameData data) {
    return Properties.getChooseAaCasualties(data);
  }

  /**
   * @return Can the attacker retreat non-amphibious units.
   */
  private static boolean isPartialAmphibiousRetreat(final GameData data) {
    return Properties.getPartialAmphibiousRetreat(data);
  }

  // nothing but static
  private BattleCalculator() {}

  /**
   * This returns the exact Power that a unit has according to what DiceRoll.rollDiceLowLuck() would give it.
   * As such, it needs to exactly match DiceRoll, otherwise this method will become useless.
   * It does NOT take into account SUPPORT.
   * It DOES take into account ROLLS.
   * It needs to be updated to take into account isMarine.
   */
  public static int getUnitPowerForSorting(final Unit current, final boolean defending, final GameData data,
      final Collection<TerritoryEffect> territoryEffects) {
    final boolean lhtrBombers = Properties.getLhtrHeavyBombers(data);
    final UnitAttachment ua = UnitAttachment.get(current.getType());
    final int rolls = (defending) ? ua.getDefenseRolls(current.getOwner()) : ua.getAttackRolls(current.getOwner());
    int strengthWithoutSupport = 0;
    // Find the strength the unit has without support
    // lhtr heavy bombers take best of n dice for both attack and defense
    if ((rolls > 1) && (lhtrBombers || ua.getChooseBestRoll())) {
      strengthWithoutSupport = (defending) ? ua.getDefense(current.getOwner()) : ua.getAttack(current.getOwner());
      strengthWithoutSupport +=
          TerritoryEffectHelper.getTerritoryCombatBonus(current.getType(), territoryEffects, defending);
      // just add one like LL if we are LHTR bombers
      strengthWithoutSupport = Math.min(Math.max(strengthWithoutSupport + 1, 0), data.getDiceSides());
    } else {
      for (int i = 0; i < rolls; i++) {
        final int tempStrength = (defending) ? ua.getDefense(current.getOwner()) : ua.getAttack(current.getOwner());
        strengthWithoutSupport +=
            TerritoryEffectHelper.getTerritoryCombatBonus(current.getType(), territoryEffects, defending);
        strengthWithoutSupport += Math.min(Math.max(tempStrength, 0), data.getDiceSides());
      }
    }
    return strengthWithoutSupport;
  }
}
