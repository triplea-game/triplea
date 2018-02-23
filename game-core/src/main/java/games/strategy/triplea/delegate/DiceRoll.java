package games.strategy.triplea.delegate;

import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.LinkedIntegerMap;
import games.strategy.util.Triple;
import games.strategy.util.Tuple;

/**
 * Used to store information about a dice roll.
 *
 * <p>
 * # of rolls at 5, at 4, etc.
 * </p>
 *
 * <p>
 * Externalizable so we can efficiently write out our dice as ints rather than as full objects.
 * </p>
 */
public class DiceRoll implements Externalizable {
  private static final long serialVersionUID = -1167204061937566271L;
  private List<Die> rolls;
  // this does not need to match the Die with isHit true
  // since for low luck we get many hits with few dice
  private int hits;
  private double expectedHits;

  /**
   * Returns a Tuple with 2 values, the first is the max attack, the second is the max dice sides for the AA unit with
   * that attack value.
   */
  public static Tuple<Integer, Integer> getAAattackAndMaxDiceSides(final Collection<Unit> defendingEnemyAa,
      final GameData data, final boolean defending) {
    int highestAttack = 0;
    final int diceSize = data.getDiceSides();
    int chosenDiceSize = diceSize;
    for (final Unit u : defendingEnemyAa) {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      int uaDiceSides = defending ? ua.getAttackAAmaxDieSides() : ua.getOffensiveAttackAAmaxDieSides();
      if (uaDiceSides < 1) {
        uaDiceSides = diceSize;
      }
      int attack = defending ? ua.getAttackAA(u.getOwner()) : ua.getOffensiveAttackAA(u.getOwner());
      if (attack > uaDiceSides) {
        attack = uaDiceSides;
      }
      if ((((float) attack) / ((float) uaDiceSides)) > (((float) highestAttack) / ((float) chosenDiceSize))) {
        highestAttack = attack;
        chosenDiceSize = uaDiceSides;
      }
    }
    if ((highestAttack > (chosenDiceSize / 2)) && (chosenDiceSize > 1)) {
      // TODO: sadly the whole low luck section falls apart if AA are hitting at greater than half the
      // value of dice, and I don't feel like rewriting it
      highestAttack = chosenDiceSize / 2;
    }
    return Tuple.of(highestAttack, chosenDiceSize);
  }

  static int getTotalAAattacks(final Collection<Unit> defendingEnemyAa,
      final Collection<Unit> validAttackingUnitsForThisRoll) {
    if (defendingEnemyAa.isEmpty() || validAttackingUnitsForThisRoll.isEmpty()) {
      return 0;
    }
    int totalAAattacksNormal = 0;
    int totalAAattacksSurplus = 0;
    for (final Unit aa : defendingEnemyAa) {
      final UnitAttachment ua = UnitAttachment.get(aa.getType());
      if (ua.getMaxAAattacks() == -1) {
        totalAAattacksNormal = validAttackingUnitsForThisRoll.size();
      } else {
        if (ua.getMayOverStackAA()) {
          totalAAattacksSurplus += ua.getMaxAAattacks();
        } else {
          totalAAattacksNormal += ua.getMaxAAattacks();
        }
      }
    }
    totalAAattacksNormal = Math.min(totalAAattacksNormal, validAttackingUnitsForThisRoll.size());
    return totalAAattacksNormal + totalAAattacksSurplus;
  }

  static DiceRoll rollAa(final Collection<Unit> validAttackingUnitsForThisRoll,
      final Collection<Unit> defendingAaForThisRoll, final IDelegateBridge bridge, final Territory location,
      final boolean defending) {
    {
      final Set<Unit> duplicatesCheckSet1 = new HashSet<>(validAttackingUnitsForThisRoll);
      if (validAttackingUnitsForThisRoll.size() != duplicatesCheckSet1.size()) {
        throw new IllegalStateException("Duplicate Units Detected: Original List:" + validAttackingUnitsForThisRoll
            + "  HashSet:" + duplicatesCheckSet1);
      }
      final Set<Unit> duplicatesCheckSet2 = new HashSet<>(defendingAaForThisRoll);
      if (defendingAaForThisRoll.size() != duplicatesCheckSet2.size()) {
        throw new IllegalStateException(
            "Duplicate Units Detected: Original List:" + defendingAaForThisRoll + "  HashSet:" + duplicatesCheckSet2);
      }
    }
    final List<Unit> defendingAa = CollectionUtils.getMatches(defendingAaForThisRoll,
        (defending ? Matches.unitAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero()
            : Matches.unitOffensiveAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero()));
    if (defendingAa.isEmpty()) {
      return new DiceRoll(new ArrayList<>(0), 0, 0);
    }
    final GameData data = bridge.getData();
    final int totalAAattacksTotal = getTotalAAattacks(defendingAa, validAttackingUnitsForThisRoll);
    if (totalAAattacksTotal <= 0) {
      return new DiceRoll(new ArrayList<>(0), 0, 0);
    }
    // determine dicesides for everyone (we are not going to consider the possibility of different dicesides within the
    // same typeAA)
    final Tuple<Integer, Integer> attackThenDiceSidesForAll = getAAattackAndMaxDiceSides(defendingAa, data, defending);
    // final int highestAttackPower = attackThenDiceSidesForAll.getFirst();
    final int chosenDiceSizeForAll = attackThenDiceSidesForAll.getSecond();
    int hits = 0;
    final List<Die> sortedDice = new ArrayList<>();
    final String typeAa = UnitAttachment.get(defendingAa.get(0).getType()).getTypeAA();
    // LOW LUCK
    final Triple<Integer, Integer, Boolean> triple = getTotalAaPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(
        null, null, defending, defendingAa, validAttackingUnitsForThisRoll, data, false);
    final int totalPower = triple.getFirst();
    if (Properties.getLowLuck(data) || Properties.getLowLuckAaOnly(data)) {
      final String annotation = "Roll " + typeAa + " in " + location.getName();
      hits += getLowLuckHits(bridge, sortedDice, totalPower, chosenDiceSizeForAll, defendingAa.get(0).getOwner(),
          annotation);
    } else {
      final String annotation = "Roll " + typeAa + " in " + location.getName();
      final int[] dice = bridge.getRandom(chosenDiceSizeForAll, totalAAattacksTotal, defendingAa.get(0).getOwner(),
          DiceType.COMBAT, annotation);
      hits += getTotalAaPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(dice, sortedDice, defending, defendingAa,
          validAttackingUnitsForThisRoll, data, true).getSecond();
    }
    final double expectedHits = ((double) totalPower) / chosenDiceSizeForAll;
    final DiceRoll roll = new DiceRoll(sortedDice, hits, expectedHits);
    final String annotation = typeAa + " fire in " + location + " : " + MyFormatter.asDice(roll);
    bridge.getHistoryWriter().addChildToEvent(annotation, roll);
    return roll;
  }

  /**
   * Basically I wanted 1 single method for both Low Luck and Dice, because if we have 2 methods then there is a chance
   * they will go out of
   * sync.
   *
   * @param dice
   *        = Rolled Dice numbers from bridge. Can be null if we do not want to return hits or fill the sortedDice
   * @param sortedDice
   *        List of dice we are recording. Can be null if we do not want to return hits or fill the sortedDice
   * @return an object containing 3 things: first is the total power of the defendingAA who will be rolling, second is
   *         number of hits,
   *         third is true/false are all rolls using the same hitAt (example: if all the rolls are at 1, we would return
   *         true, but if one
   *         roll is at 1 and another roll is at 2, then we return false)
   */
  public static Triple<Integer, Integer, Boolean> getTotalAaPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(
      final int[] dice, final List<Die> sortedDice, final boolean defending,
      final Collection<Unit> defendingAaForThisRoll, final Collection<Unit> validAttackingUnitsForThisRoll,
      final GameData data, final boolean fillInSortedDiceAndRecordHits) {
    final List<Unit> defendingAa = CollectionUtils.getMatches(defendingAaForThisRoll,
        (defending ? Matches.unitAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero()
            : Matches.unitOffensiveAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero()));
    if (defendingAa.size() <= 0) {
      return Triple.of(0, 0, false);
    }
    // we want to make sure the higher powers fire
    sortAaHighToLow(defendingAa, data, defending);
    // this is confusing, but what we want to do is the following:
    // any aa that are NOT infinite attacks, and NOT overstack, will fire first individually ((because their
    // power/dicesides might be
    // different [example: radar tech on a german aa gun, in the same territory as an italian aagun without radar,
    // neither is infinite])
    // all aa that have "infinite attacks" will have the one with the highest power/dicesides of them all, fire at
    // whatever aa units have
    // not yet been fired at
    // HOWEVER, if the non-infinite attackers are less powerful than the infinite attacker, then the non-infinite will
    // not fire, and the
    // infinite one will do all the attacks for both groups.
    // the total number of shots from these first 2 groups cannot exceed the number of air units being shot at
    // last, any aa that can overstack will fire after, individually
    // (an aa guns that is both infinite, and overstacks, ignores the overstack part because that totally doesn't make
    // any sense)
    // set up all 3 groups of aa guns
    final List<Unit> normalNonInfiniteAa = new ArrayList<>(defendingAa);
    final List<Unit> infiniteAa = CollectionUtils.getMatches(defendingAa, Matches.unitMaxAaAttacksIsInfinite());
    final List<Unit> overstackAa = CollectionUtils.getMatches(defendingAa, Matches.unitMayOverStackAa());
    overstackAa.removeAll(infiniteAa);
    normalNonInfiniteAa.removeAll(infiniteAa);
    normalNonInfiniteAa.removeAll(overstackAa);
    // determine maximum total attacks
    final int totalAAattacksTotal = getTotalAAattacks(defendingAa, validAttackingUnitsForThisRoll);
    // determine individual totals
    final int normalNonInfiniteAAtotalAAattacks =
        getTotalAAattacks(normalNonInfiniteAa, validAttackingUnitsForThisRoll);
    final int infiniteAAtotalAAattacks =
        Math.min((validAttackingUnitsForThisRoll.size() - normalNonInfiniteAAtotalAAattacks),
            getTotalAAattacks(infiniteAa, validAttackingUnitsForThisRoll));
    final int overstackAAtotalAAattacks = getTotalAAattacks(overstackAa, validAttackingUnitsForThisRoll);
    if (totalAAattacksTotal != (normalNonInfiniteAAtotalAAattacks + infiniteAAtotalAAattacks
        + overstackAAtotalAAattacks)) {
      throw new IllegalStateException("Total attacks should be: " + totalAAattacksTotal + " but instead is: "
          + (normalNonInfiniteAAtotalAAattacks + infiniteAAtotalAAattacks + overstackAAtotalAAattacks));
      // determine dicesides for everyone (we are not going to consider the possibility of different dicesides within
      // the same typeAA)
      // final Tuple<Integer, Integer> attackThenDiceSidesForAll = getAAattackAndMaxDiceSides(defendingAA, data);
      // final int chosenDiceSizeForAll = attackThenDiceSidesForAll.getSecond();
    }
    // determine highest attack for infinite group
    final Tuple<Integer, Integer> attackThenDiceSidesForInfinite =
        getAAattackAndMaxDiceSides(infiniteAa, data, defending);
    // not zero based
    final int hitAtForInfinite = attackThenDiceSidesForInfinite.getFirst();
    // not zero based
    // final int powerForInfinite = highestAttackForInfinite;
    // if we are low luck, we only want to know the power and total attacks, while if we are dice we will be filling the
    // sorted dice
    final boolean recordSortedDice =
        fillInSortedDiceAndRecordHits && (dice != null) && (dice.length > 0) && (sortedDice != null);
    int totalPower = 0;
    int hits = 0;
    int i = 0;
    final Set<Integer> rolledAt = new HashSet<>();
    // non-infinite, non-overstack aa
    int runningMaximum = normalNonInfiniteAAtotalAAattacks;
    final Iterator<Unit> normalAAiter = normalNonInfiniteAa.iterator();
    while ((i < runningMaximum) && normalAAiter.hasNext()) {
      final Unit aaGun = normalAAiter.next();
      // should be > 0 at this point
      int numAttacks = UnitAttachment.get(aaGun.getType()).getMaxAAattacks();
      final int hitAt = getAAattackAndMaxDiceSides(Collections.singleton(aaGun), data, defending).getFirst();
      if (hitAt < hitAtForInfinite) {
        continue;
      }
      while ((i < runningMaximum) && (numAttacks > 0)) {
        if (recordSortedDice) {
          // dice are zero based
          final boolean hit = dice[i] < hitAt;
          sortedDice.add(new Die(dice[i], hitAt, hit ? DieType.HIT : DieType.MISS));
          if (hit) {
            hits++;
          }
        }
        i++;
        numAttacks--;
        totalPower += hitAt;
        rolledAt.add(hitAt);
      }
    }
    // infinite aa
    runningMaximum += infiniteAAtotalAAattacks;
    while (i < runningMaximum) {
      // we use the highest attack of this group, since each is infinite. (this is the default behavior in revised)
      if (recordSortedDice) {
        // dice are zero based
        final boolean hit = dice[i] < hitAtForInfinite;
        sortedDice.add(new Die(dice[i], hitAtForInfinite, hit ? DieType.HIT : DieType.MISS));
        if (hit) {
          hits++;
        }
      }
      i++;
      totalPower += hitAtForInfinite;
      rolledAt.add(hitAtForInfinite);
    }
    // overstack aa
    runningMaximum += overstackAAtotalAAattacks;
    final Iterator<Unit> overstackAAiter = overstackAa.iterator();
    while ((i < runningMaximum) && overstackAAiter.hasNext()) {
      final Unit aaGun = overstackAAiter.next();
      // should be > 0 at this point
      int numAttacks = UnitAttachment.get(aaGun.getType()).getMaxAAattacks();
      // zero based, so subtract 1
      final int hitAt = getAAattackAndMaxDiceSides(Collections.singleton(aaGun), data, defending).getFirst();
      while ((i < runningMaximum) && (numAttacks > 0)) {
        if (recordSortedDice) {
          // dice are zero based
          final boolean hit = dice[i] < hitAt;
          sortedDice.add(new Die(dice[i], hitAt, hit ? DieType.HIT : DieType.MISS));
          if (hit) {
            hits++;
          }
        }
        i++;
        numAttacks--;
        totalPower += hitAt;
        rolledAt.add(hitAt);
      }
    }
    return Triple.of(totalPower, hits, (rolledAt.size() == 1));
  }

  private static void sortAaHighToLow(final List<Unit> units, final GameData data, final boolean defending) {
    final Comparator<Unit> comparator = (u1, u2) -> {
      final Tuple<Integer, Integer> tuple1 = getAAattackAndMaxDiceSides(Collections.singleton(u1), data, defending);
      final Tuple<Integer, Integer> tuple2 = getAAattackAndMaxDiceSides(Collections.singleton(u2), data, defending);
      if (tuple1.getFirst() == 0) {
        if (tuple2.getFirst() == 0) {
          return 0;
        }
        return 1;
      } else if (tuple2.getFirst() == 0) {
        return -1;
      }
      final float value1 = ((float) tuple1.getFirst()) / ((float) tuple1.getSecond());
      final float value2 = ((float) tuple2.getFirst()) / ((float) tuple2.getSecond());
      if (value1 < value2) {
        return 1;
      } else if (value1 > value2) {
        return -1;
      }
      return 0;
    };
    Collections.sort(units, comparator);
  }

  private static int getLowLuckHits(final IDelegateBridge bridge, final List<Die> sortedDice, final int totalPower,
      final int chosenDiceSize, final PlayerID playerRolling, final String annotation) {
    int hits = totalPower / chosenDiceSize;
    final int hitsFractional = totalPower % chosenDiceSize;
    if (hitsFractional > 0) {
      final int[] dice = bridge.getRandom(chosenDiceSize, 1, playerRolling, DiceType.COMBAT, annotation);
      final boolean hit = hitsFractional > dice[0];
      if (hit) {
        hits++;
      }
      final Die die = new Die(dice[0], hitsFractional, hit ? DieType.HIT : DieType.MISS);
      sortedDice.add(die);
    }
    return hits;
  }

  /**
   * Roll dice for units.
   */
  public static DiceRoll rollDice(final List<Unit> units, final boolean defending, final PlayerID player,
      final IDelegateBridge bridge, final IBattle battle, final String annotation,
      final Collection<TerritoryEffect> territoryEffects, final List<Unit> allEnemyUnitsAliveOrWaitingToDie) {
    // Decide whether to use low luck rules or normal rules.
    if (Properties.getLowLuck(bridge.getData())) {
      return rollDiceLowLuck(units, defending, player, bridge, battle, annotation, territoryEffects,
          allEnemyUnitsAliveOrWaitingToDie);
    }
    return rollDiceNormal(units, defending, player, bridge, battle, annotation, territoryEffects,
        allEnemyUnitsAliveOrWaitingToDie);
  }

  /**
   * Roll n-sided dice.
   *
   * @param annotation
   *        0 based, add 1 to get actual die roll
   */
  public static DiceRoll rollNDice(final IDelegateBridge bridge, final int rollCount, final int sides,
      final PlayerID playerRolling, final DiceType diceType, final String annotation) {
    if (rollCount == 0) {
      return new DiceRoll(new ArrayList<>(), 0, 0);
    }
    final int[] random = bridge.getRandom(sides, rollCount, playerRolling, diceType, annotation);
    final List<Die> dice = new ArrayList<>();
    for (int i = 0; i < rollCount; i++) {
      dice.add(new Die(random[i], 1, DieType.IGNORED));
    }
    return new DiceRoll(dice, rollCount, rollCount);
  }

  /**
   * @param unitsGettingPowerFor
   *        should be sorted from weakest to strongest, before the method is called, for the actual battle.
   */
  public static Map<Unit, Tuple<Integer, Integer>> getUnitPowerAndRollsForNormalBattles(
      final List<Unit> unitsGettingPowerFor, final List<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final boolean defending, final boolean bombing, final GameData data, final Territory location,
      final Collection<TerritoryEffect> territoryEffects, final boolean isAmphibiousBattle,
      final Collection<Unit> amphibiousLandAttackers) {

    return getUnitPowerAndRollsForNormalBattles(unitsGettingPowerFor,
        allEnemyUnitsAliveOrWaitingToDie, defending, bombing, data, location, territoryEffects,
        isAmphibiousBattle, amphibiousLandAttackers, new HashMap<>(),
        new HashMap<>());
  }

  /**
   * @param unitsGettingPowerFor
   *        should be sorted from weakest to strongest, before the method is called, for the actual battle.
   */
  protected static Map<Unit, Tuple<Integer, Integer>> getUnitPowerAndRollsForNormalBattles(
      final List<Unit> unitsGettingPowerFor,
      final List<Unit> allEnemyUnitsAliveOrWaitingToDie, final boolean defending, final boolean bombing,
      final GameData data, final Territory location,
      final Collection<TerritoryEffect> territoryEffects, final boolean isAmphibiousBattle,
      final Collection<Unit> amphibiousLandAttackers, final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap,
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap) {
    final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRolls = new HashMap<>();
    if ((unitsGettingPowerFor == null) || unitsGettingPowerFor.isEmpty()) {
      return unitPowerAndRolls;
    }
    // get all supports, friendly and enemy
    final Set<List<UnitSupportAttachment>> supportRulesFriendly = new HashSet<>();
    final IntegerMap<UnitSupportAttachment> supportLeftFriendly = new IntegerMap<>();
    final Map<UnitSupportAttachment, LinkedIntegerMap<Unit>> supportUnitsLeftFriendly =
        new HashMap<>();
    getSupport(unitsGettingPowerFor, supportRulesFriendly, supportLeftFriendly, supportUnitsLeftFriendly,
        data, defending, true);
    final Set<List<UnitSupportAttachment>> supportRulesEnemy = new HashSet<>();
    final IntegerMap<UnitSupportAttachment> supportLeftEnemy = new IntegerMap<>();
    final Map<UnitSupportAttachment, LinkedIntegerMap<Unit>> supportUnitsLeftEnemy =
        new HashMap<>();
    getSupport(allEnemyUnitsAliveOrWaitingToDie, supportRulesEnemy, supportLeftEnemy, supportUnitsLeftEnemy, data,
        !defending, false);
    // copy for rolls
    final IntegerMap<UnitSupportAttachment> supportLeftFriendlyRolls =
        new IntegerMap<>(supportLeftFriendly);
    final IntegerMap<UnitSupportAttachment> supportLeftEnemyRolls =
        new IntegerMap<>(supportLeftEnemy);
    final Map<UnitSupportAttachment, LinkedIntegerMap<Unit>> supportUnitsLeftFriendlyRolls =
        new HashMap<>();
    for (final UnitSupportAttachment usa : supportUnitsLeftFriendly.keySet()) {
      supportUnitsLeftFriendlyRolls.put(usa, new LinkedIntegerMap<>(supportUnitsLeftFriendly.get(usa)));
    }
    final Map<UnitSupportAttachment, LinkedIntegerMap<Unit>> supportUnitsLeftEnemyRolls =
        new HashMap<>();
    for (final UnitSupportAttachment usa : supportUnitsLeftEnemy.keySet()) {
      supportUnitsLeftEnemyRolls.put(usa, new LinkedIntegerMap<>(supportUnitsLeftEnemy.get(usa)));
    }
    final int diceSides = data.getDiceSides();
    for (final Unit current : unitsGettingPowerFor) {
      // find our initial strength
      int strength;
      final UnitAttachment ua = UnitAttachment.get(current.getType());
      if (defending) {
        strength = ua.getDefense(current.getOwner());
        if (isFirstTurnLimitedRoll(current.getOwner(), data)) {
          strength = Math.min(1, strength);
        } else {
          strength += getSupport(current, supportRulesFriendly, supportLeftFriendly, supportUnitsLeftFriendly,
              unitSupportPowerMap, true, false);
        }
        strength += getSupport(current, supportRulesEnemy, supportLeftEnemy, supportUnitsLeftEnemy, unitSupportPowerMap,
            true, false);
      } else {
        strength = ua.getAttack(current.getOwner());
        if ((ua.getIsMarine() != 0) && isAmphibiousBattle) {
          if (amphibiousLandAttackers.contains(current)) {
            strength += ua.getIsMarine();
          }
        }
        if (ua.getIsSea() && isAmphibiousBattle && Matches.territoryIsLand().test(location)) {
          // change the strength to be bombard, not attack/defense, because this is a
          strength = ua.getBombard();
          // bombarding naval unit
        }
        strength += getSupport(current, supportRulesFriendly, supportLeftFriendly, supportUnitsLeftFriendly,
            unitSupportPowerMap, true, false);
        strength += getSupport(current, supportRulesEnemy, supportLeftEnemy, supportUnitsLeftEnemy, unitSupportPowerMap,
            true, false);
      }
      strength += TerritoryEffectHelper.getTerritoryCombatBonus(current.getType(), territoryEffects, defending);
      strength = Math.min(Math.max(strength, 0), diceSides);
      // now determine our rolls
      int rolls;
      if (!bombing && (strength == 0)) {
        rolls = 0;
      } else {
        if (defending) {
          rolls = ua.getDefenseRolls(current.getOwner());
        } else {
          rolls = ua.getAttackRolls(current.getOwner());
        }
        rolls += getSupport(current, supportRulesFriendly, supportLeftFriendlyRolls, supportUnitsLeftFriendlyRolls,
            unitSupportRollsMap, false, true);
        rolls += getSupport(current, supportRulesEnemy, supportLeftEnemyRolls, supportUnitsLeftEnemyRolls,
            unitSupportRollsMap, false, true);
        rolls = Math.max(0, rolls);
        if (rolls == 0) {
          strength = 0;
        }
      }
      unitPowerAndRolls.put(current, Tuple.of(strength, rolls));
    }
    return unitPowerAndRolls;
  }

  public static int getTotalPower(final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRollsMap,
      final GameData data) {
    return getTotalPowerAndRolls(unitPowerAndRollsMap, data).getFirst();
  }


  private static Tuple<Integer, Integer> getTotalPowerAndRolls(
      final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRollsMap, final GameData data) {
    final int diceSides = data.getDiceSides();
    final boolean lowLuck = Properties.getLowLuck(data);
    final boolean lhtrBombers = Properties.getLhtrHeavyBombers(data);
    // bonus is normally 1 for most games
    final int extraRollBonus = Math.max(1, data.getDiceSides() / 6);
    int totalPower = 0;
    int totalRolls = 0;
    for (final Entry<Unit, Tuple<Integer, Integer>> entry : unitPowerAndRollsMap.entrySet()) {
      int unitStrength = Math.min(Math.max(0, entry.getValue().getFirst()), diceSides);
      final int unitRolls = entry.getValue().getSecond();
      if ((unitStrength <= 0) || (unitRolls <= 0)) {
        continue;
      }
      if (unitRolls == 1) {
        totalPower += unitStrength;
        totalRolls += unitRolls;
      } else {
        final UnitAttachment ua = UnitAttachment.get(entry.getKey().getType());
        if (lhtrBombers || ua.getChooseBestRoll()) {
          // LHTR means pick the best dice roll, which doesn't really make sense in LL. So instead, we will just add
          // +1 onto the power to
          // simulate the gains of having the best die picked.
          unitStrength += extraRollBonus * (unitRolls - 1);
          totalPower += Math.min(unitStrength, diceSides);
          totalRolls += unitRolls;
        } else {
          totalPower += unitRolls * unitStrength;
          totalRolls += unitRolls;
        }
      }
    }

    return Tuple.of(totalPower, totalRolls);
  }

  /**
   * Roll dice for units using low luck rules. Low luck rules based on rules in DAAK.
   */
  private static DiceRoll rollDiceLowLuck(final List<Unit> unitsList, final boolean defending, final PlayerID player,
      final IDelegateBridge bridge, final IBattle battle, final String annotation,
      final Collection<TerritoryEffect> territoryEffects, final List<Unit> allEnemyUnitsAliveOrWaitingToDie) {
    final List<Unit> units = new ArrayList<>(unitsList);
    {
      final Set<Unit> duplicatesCheckSet = new HashSet<>(unitsList);
      if (units.size() != duplicatesCheckSet.size()) {
        throw new IllegalStateException(
            "Duplicate Units Detected: Original List:" + units + "  HashSet:" + duplicatesCheckSet);
      }
    }
    final GameData data = bridge.getData();
    final Territory location = battle.getTerritory();
    final boolean isAmphibiousBattle = battle.isAmphibious();
    final Collection<Unit> amphibiousLandAttackers = battle.getAmphibiousLandAttackers();
    final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRollsMap =
        DiceRoll.getUnitPowerAndRollsForNormalBattles(units, allEnemyUnitsAliveOrWaitingToDie, defending, false,
            data, location, territoryEffects, isAmphibiousBattle, amphibiousLandAttackers);
    final int power = getTotalPower(unitPowerAndRollsMap, data);
    if (power == 0) {
      return new DiceRoll(new ArrayList<>(0), 0, 0);
    }
    int hitCount = power / data.getDiceSides();
    final List<Die> dice = new ArrayList<>();
    // We need to roll dice for the fractional part of the dice.
    final int rollFor = power % data.getDiceSides();
    final int[] random;
    if (rollFor == 0) {
      random = new int[0];
    } else {
      random = bridge.getRandom(data.getDiceSides(), 1, player, DiceType.COMBAT, annotation);
      // zero based
      final boolean hit = rollFor > random[0];
      if (hit) {
        hitCount++;
      }
      dice.add(new Die(random[0], rollFor, hit ? DieType.HIT : DieType.MISS));
    }
    // Create DiceRoll object
    final double expectedHits = ((double) power) / data.getDiceSides();
    final DiceRoll diceRoll = new DiceRoll(dice, hitCount, expectedHits);
    bridge.getHistoryWriter().addChildToEvent(annotation + " : " + MyFormatter.asDice(random), diceRoll);
    return diceRoll;
  }

  /**
   * Fills a set and map with the support possibly given by these units.
   *
   * @param supportsAvailable
   *        an empty set that will be filled with all support rules grouped into lists of non-stacking rules
   * @param supportLeft
   *        an empty map that will be filled with all the support that can be given in the form of counters
   * @param supportUnitsLeft
   *        an empty map that will be filled with all the support that can be given in the form of counters
   * @param defence
   *        are the receiving units defending?
   * @param allies
   *        are the receiving units allied to the giving units?
   */
  public static void getSupport(final List<Unit> unitsGivingTheSupport,
      final Set<List<UnitSupportAttachment>> supportsAvailable, final IntegerMap<UnitSupportAttachment> supportLeft,
      final Map<UnitSupportAttachment, LinkedIntegerMap<Unit>> supportUnitsLeft, final GameData data,
      final boolean defence, final boolean allies) {
    if ((unitsGivingTheSupport == null) || unitsGivingTheSupport.isEmpty()) {
      return;
    }
    for (final UnitSupportAttachment rule : UnitSupportAttachment.get(data)) {
      if (rule.getPlayers().isEmpty()) {
        continue;
      }
      if (!((defence && rule.getDefence()) || (!defence && rule.getOffence()))) {
        continue;
      }
      if (!((allies && rule.getAllied()) || (!allies && rule.getEnemy()))) {
        continue;
      }
      final Predicate<Unit> canSupport = Matches.unitIsOfType((UnitType) rule.getAttachedTo())
          .and(Matches.unitOwnedBy(rule.getPlayers()));
      final List<Unit> supporters = CollectionUtils.getMatches(unitsGivingTheSupport, canSupport);
      int numSupport = supporters.size();
      if (numSupport <= 0) {
        continue;
      }
      final List<Unit> impArtTechUnits = new ArrayList<>();
      if (rule.getImpArtTech()) {
        impArtTechUnits
            .addAll(CollectionUtils.getMatches(supporters, Matches.unitOwnerHasImprovedArtillerySupportTech()));
      }
      numSupport += impArtTechUnits.size();
      supportLeft.put(rule, numSupport * rule.getNumber());
      supportUnitsLeft.put(rule, new LinkedIntegerMap<>(supporters, rule.getNumber()));
      supportUnitsLeft.get(rule).addAll(impArtTechUnits, rule.getNumber());
      final Iterator<List<UnitSupportAttachment>> iter2 = supportsAvailable.iterator();
      List<UnitSupportAttachment> ruleType = null;
      boolean found = false;
      final String bonusType = rule.getBonusType();
      while (iter2.hasNext()) {
        ruleType = iter2.next();
        if (ruleType.get(0).getBonusType().equals(bonusType)) {
          found = true;
          break;
        }
      }
      if (!found) {
        ruleType = new ArrayList<>();
        supportsAvailable.add(ruleType);
      }
      if (ruleType != null) {
        ruleType.add(rule);
      }
    }
    sortSupportRules(supportsAvailable, defence, allies);
  }

  /**
   * Returns the support for this unit type, and decrements the supportLeft counters.
   *
   * @return the bonus given to the unit
   */
  public static int getSupport(final Unit unit, final Set<List<UnitSupportAttachment>> supportsAvailable,
      final IntegerMap<UnitSupportAttachment> supportLeft,
      final Map<UnitSupportAttachment, LinkedIntegerMap<Unit>> supportUnitsLeft,
      final Map<Unit, IntegerMap<Unit>> unitSupportMap, final boolean strength, final boolean rolls) {
    int givenSupport = 0;
    for (final List<UnitSupportAttachment> bonusType : supportsAvailable) {
      for (final UnitSupportAttachment rule : bonusType) {
        if (!((strength && rule.getStrength()) || (rolls && rule.getRoll()))) {
          continue;
        }
        final Set<UnitType> types = rule.getUnitType();
        if ((types != null) && types.contains(unit.getType()) && (supportLeft.getInt(rule) > 0)) {
          givenSupport += rule.getBonus();
          supportLeft.add(rule, -1);
          final LinkedIntegerMap<Unit> supportersLeft = supportUnitsLeft.get(rule);
          if (supportersLeft != null) {
            final Set<Unit> supporters = supportersLeft.keySet();
            if (!supporters.isEmpty()) {
              final Unit u = supporters.iterator().next();
              supportUnitsLeft.get(rule).add(u, -1);
              if (supportUnitsLeft.get(rule).getInt(u) <= 0) {
                supportUnitsLeft.get(rule).removeKey(u);
              }
              if (unitSupportMap.containsKey(u)) {
                unitSupportMap.get(u).add(unit, rule.getBonus());
              } else {
                unitSupportMap.put(u, new IntegerMap<>(unit, rule.getBonus()));
              }
            }
          }
          break;
        }
      }
    }
    return givenSupport;
  }

  public static void sortByStrength(final List<Unit> units, final boolean defending) {
    final Comparator<Unit> comp = (u1, u2) -> {
      final Integer v1;
      final Integer v2;
      if (defending) {
        v1 = UnitAttachment.get(u1.getType()).getDefense(u1.getOwner());
        v2 = UnitAttachment.get(u2.getType()).getDefense(u2.getOwner());
      } else {
        v1 = UnitAttachment.get(u1.getType()).getAttack(u1.getOwner());
        v2 = UnitAttachment.get(u2.getType()).getAttack(u2.getOwner());
      }
      return v1.compareTo(v2);
    };
    Collections.sort(units, comp);
  }

  private static void sortSupportRules(final Set<List<UnitSupportAttachment>> support, final boolean defense,
      final boolean friendly) {
    // first, sort the lists inside each set
    final Comparator<UnitSupportAttachment> compList = (u1, u2) -> {
      int compareTo;
      // we want to apply the biggest bonus first
      // Make sure stronger supports are ordered first if friendly, and worst are ordered first if enemy
      // TODO: it is possible that we will waste negative support if we reduce a units power to less than zero.
      // We should actually apply enemy negative support in order from worst to least bad, on a unit list that is
      // ordered from strongest
      // to weakest.
      final boolean u1CanBonus = defense ? u1.getDefence() : u1.getOffence();
      final boolean u2CanBonus = defense ? u2.getDefence() : u2.getOffence();
      if (friendly) {
        // favor rolls over strength
        if (u1.getRoll() || u2.getRoll()) {
          final int u1Bonus = (u1.getRoll() && u1CanBonus) ? u1.getBonus() : 0;
          final Integer u2Bonus = (u2.getRoll() && u2CanBonus) ? u2.getBonus() : 0;
          compareTo = u2Bonus.compareTo(u1Bonus);
          if (compareTo != 0) {
            return compareTo;
          }
        }
        if (u1.getStrength() || u2.getStrength()) {
          final int u1Bonus = (u1.getStrength() && u1CanBonus) ? u1.getBonus() : 0;
          final Integer u2Bonus = (u2.getStrength() && u2CanBonus) ? u2.getBonus() : 0;
          compareTo = u2Bonus.compareTo(u1Bonus);
          if (compareTo != 0) {
            return compareTo;
          }
        }
      } else {
        if (u1.getRoll() || u2.getRoll()) {
          final Integer u1Bonus = (u1.getRoll() && u1CanBonus) ? u1.getBonus() : 0;
          final int u2Bonus = (u2.getRoll() && u2CanBonus) ? u2.getBonus() : 0;
          compareTo = u1Bonus.compareTo(u2Bonus);
          if (compareTo != 0) {
            return compareTo;
          }
        }
        if (u1.getStrength() || u2.getStrength()) {
          final Integer u1Bonus = (u1.getStrength() && u1CanBonus) ? u1.getBonus() : 0;
          final int u2Bonus = (u2.getStrength() && u2CanBonus) ? u2.getBonus() : 0;
          compareTo = u1Bonus.compareTo(u2Bonus);
          if (compareTo != 0) {
            return compareTo;
          }
        }
      }
      // if the bonuses are the same, we want to make sure any support which only supports 1 single unittype goes
      // first
      // the reason being that we could have Support1 which supports both infantry and mech infantry, and Support2
      // which only supports
      // mech infantry
      // if the Support1 goes first, and the mech infantry is first in the unit list (highly probable), then Support1
      // will end up using
      // all of itself up on the mech infantry
      // then when the Support2 comes up, all the mech infantry are used up, and it does nothing.
      // instead, we want Support2 to come first, support all mech infantry that it can, then have Support1 come in
      // and support whatever
      // is left, that way no support is wasted
      // TODO: this breaks down completely if we have Support1 having a higher bonus than Support2, because it will
      // come first. It should
      // come first, unless we would have support wasted otherwise. This ends up being a pretty tricky math puzzle.
      final Set<UnitType> types1 = u1.getUnitType();
      final Set<UnitType> types2 = u2.getUnitType();
      final Integer s1 = (types1 == null) ? 0 : types1.size();
      final int s2 = (types2 == null) ? 0 : types2.size();
      compareTo = s1.compareTo(s2);
      if (compareTo != 0) {
        return compareTo;
      }
      // Now we need to sort so that the supporters who are the most powerful go before the less powerful
      // This is not necessary for the providing of support, but is necessary for our default casualty selection
      // method
      final UnitType unitType1 = (UnitType) u1.getAttachedTo();
      final UnitType unitType2 = (UnitType) u2.getAttachedTo();
      final UnitAttachment ua1 = UnitAttachment.get(unitType1);
      final UnitAttachment ua2 = UnitAttachment.get(unitType2);
      final int unitPower1;
      final Integer unitPower2;
      if (u1.getDefence()) {
        unitPower1 = ua1.getDefenseRolls(PlayerID.NULL_PLAYERID) * ua1.getDefense(PlayerID.NULL_PLAYERID);
        unitPower2 = ua2.getDefenseRolls(PlayerID.NULL_PLAYERID) * ua2.getDefense(PlayerID.NULL_PLAYERID);
      } else {
        unitPower1 = ua1.getAttackRolls(PlayerID.NULL_PLAYERID) * ua1.getAttack(PlayerID.NULL_PLAYERID);
        unitPower2 = ua2.getAttackRolls(PlayerID.NULL_PLAYERID) * ua2.getAttack(PlayerID.NULL_PLAYERID);
      }
      return unitPower2.compareTo(unitPower1);
    };
    for (final List<UnitSupportAttachment> attachments : support) {
      Collections.sort(attachments, compList);
    }
  }

  static DiceRoll airBattle(final List<Unit> unitsList, final boolean defending, final PlayerID player,
      final IDelegateBridge bridge, final String annotation) {
    {
      final Set<Unit> duplicatesCheckSet1 = new HashSet<>(unitsList);
      if (unitsList.size() != duplicatesCheckSet1.size()) {
        throw new IllegalStateException(
            "Duplicate Units Detected: Original List:" + unitsList + "  HashSet:" + duplicatesCheckSet1);
      }
    }
    final GameData data = bridge.getData();
    final boolean lhtrBombers = Properties.getLhtrHeavyBombers(data);
    final List<Unit> units = new ArrayList<>(unitsList);
    final int rollCount = AirBattle.getAirBattleRolls(unitsList, defending);
    if (rollCount == 0) {
      return new DiceRoll(new ArrayList<>(), 0, 0);
    }
    int[] random;
    final List<Die> dice = new ArrayList<>();
    int hitCount = 0;

    // bonus is normally 1 for most games
    final int extraRollBonus = Math.max(1, data.getDiceSides() / 6);
    int totalPower = 0;
    // We iterate through the units to find the total strength of the units
    for (final Unit current : units) {
      final UnitAttachment ua = UnitAttachment.get(current.getType());
      final int rolls = AirBattle.getAirBattleRolls(current, defending);
      int totalStrength = 0;
      final int strength = Math.min(data.getDiceSides(),
          Math.max(0, (defending ? ua.getAirDefense(current.getOwner()) : ua.getAirAttack(current.getOwner()))));
      for (int i = 0; i < rolls; i++) {
        // LHTR means pick the best dice roll, which doesn't really make sense in LL. So instead, we will just add +1
        // onto the power to
        // simulate the gains of having the best die picked.
        if ((i > 1) && (lhtrBombers || ua.getChooseBestRoll())) {
          totalStrength += extraRollBonus;
          continue;
        }
        totalStrength += strength;
      }
      totalPower += Math.min(Math.max(totalStrength, 0), data.getDiceSides());
    }

    if (Properties.getLowLuck(data)) {
      // Get number of hits
      hitCount = totalPower / data.getDiceSides();
      random = new int[0];
      // We need to roll dice for the fractional part of the dice.
      final int power = totalPower % data.getDiceSides();
      if (power != 0) {
        random = bridge.getRandom(data.getDiceSides(), 1, player, DiceType.COMBAT, annotation);
        final boolean hit = power > random[0];
        if (hit) {
          hitCount++;
        }
        dice.add(new Die(random[0], power, hit ? DieType.HIT : DieType.MISS));
      }
    } else {
      random = bridge.getRandom(data.getDiceSides(), rollCount, player, DiceType.COMBAT, annotation);
      int diceIndex = 0;
      for (final Unit current : units) {
        final UnitAttachment ua = UnitAttachment.get(current.getType());
        final int strength = Math.min(data.getDiceSides(),
            Math.max(0, (defending ? ua.getAirDefense(current.getOwner()) : ua.getAirAttack(current.getOwner()))));
        final int rolls = AirBattle.getAirBattleRolls(current, defending);
        // lhtr heavy bombers take best of n dice for both attack and defense
        if ((rolls > 1) && (lhtrBombers || ua.getChooseBestRoll())) {
          int minIndex = 0;
          int min = data.getDiceSides();
          for (int i = 0; i < rolls; i++) {
            if (random[diceIndex + i] < min) {
              min = random[diceIndex + i];
              minIndex = i;
            }
          }
          final boolean hit = strength > random[diceIndex + minIndex];
          dice.add(new Die(random[diceIndex + minIndex], strength, hit ? DieType.HIT : DieType.MISS));
          for (int i = 0; i < rolls; i++) {
            if (i != minIndex) {
              dice.add(new Die(random[diceIndex + i], strength, DieType.IGNORED));
            }
          }
          if (hit) {
            hitCount++;
          }
          diceIndex += rolls;
        } else {
          for (int i = 0; i < rolls; i++) {
            final boolean hit = strength > random[diceIndex];
            dice.add(new Die(random[diceIndex], strength, hit ? DieType.HIT : DieType.MISS));
            if (hit) {
              hitCount++;
            }
            diceIndex++;
          }
        }
      }
    }
    final double expectedHits = ((double) totalPower) / data.getDiceSides();
    final DiceRoll diceRoll = new DiceRoll(dice, hitCount, expectedHits);
    bridge.getHistoryWriter().addChildToEvent(annotation + " : " + MyFormatter.asDice(random), diceRoll);
    return diceRoll;
  }

  /**
   * Roll dice for units per normal rules.
   */
  private static DiceRoll rollDiceNormal(final List<Unit> unitsList, final boolean defending, final PlayerID player,
      final IDelegateBridge bridge, final IBattle battle, final String annotation,
      final Collection<TerritoryEffect> territoryEffects, final List<Unit> allEnemyUnitsAliveOrWaitingToDie) {
    final List<Unit> units = new ArrayList<>(unitsList);
    {
      final Set<Unit> duplicatesCheckSet = new HashSet<>(unitsList);
      if (units.size() != duplicatesCheckSet.size()) {
        throw new IllegalStateException(
            "Duplicate Units Detected: Original List:" + units + "  HashSet:" + duplicatesCheckSet);
      }
    }
    final GameData data = bridge.getData();
    sortByStrength(units, defending);
    final Territory location = battle.getTerritory();
    final boolean isAmphibiousBattle = battle.isAmphibious();
    final Collection<Unit> amphibiousLandAttackers = battle.getAmphibiousLandAttackers();
    final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRollsMap =
        DiceRoll.getUnitPowerAndRollsForNormalBattles(units, allEnemyUnitsAliveOrWaitingToDie, defending, false,
            data, location, territoryEffects, isAmphibiousBattle, amphibiousLandAttackers);
    final Tuple<Integer, Integer> totalPowerAndRolls = getTotalPowerAndRolls(unitPowerAndRollsMap, data);
    final int totalPower = totalPowerAndRolls.getFirst();
    final int rollCount = totalPowerAndRolls.getSecond();
    if (rollCount == 0) {
      return new DiceRoll(new ArrayList<>(), 0, 0);
    }
    final int[] random = bridge.getRandom(data.getDiceSides(), rollCount, player, DiceType.COMBAT, annotation);
    final boolean lhtrBombers = Properties.getLhtrHeavyBombers(data);
    final List<Die> dice = new ArrayList<>();
    int hitCount = 0;
    int diceIndex = 0;
    for (final Unit current : units) {
      final UnitAttachment ua = UnitAttachment.get(current.getType());
      final Tuple<Integer, Integer> powerAndRolls = unitPowerAndRollsMap.get(current);
      final int strength = powerAndRolls.getFirst();
      final int rolls = powerAndRolls.getSecond();
      // lhtr heavy bombers take best of n dice for both attack and defense
      if ((rolls <= 0) || (strength <= 0)) {
        continue;
      }
      if ((rolls > 1) && (lhtrBombers || ua.getChooseBestRoll())) {
        int smallestDieIndex = 0;
        int smallestDie = data.getDiceSides();
        for (int i = 0; i < rolls; i++) {
          if (random[diceIndex + i] < smallestDie) {
            smallestDie = random[diceIndex + i];
            smallestDieIndex = i;
          }
        }
        // zero based
        final boolean hit = strength > random[diceIndex + smallestDieIndex];
        dice.add(new Die(random[diceIndex + smallestDieIndex], strength, hit ? DieType.HIT : DieType.MISS));
        for (int i = 0; i < rolls; i++) {
          if (i != smallestDieIndex) {
            dice.add(new Die(random[diceIndex + i], strength, DieType.IGNORED));
          }
        }
        if (hit) {
          hitCount++;
        }
        diceIndex += rolls;
      } else {
        for (int i = 0; i < rolls; i++) {
          // zero based
          final boolean hit = strength > random[diceIndex];
          dice.add(new Die(random[diceIndex], strength, hit ? DieType.HIT : DieType.MISS));
          if (hit) {
            hitCount++;
          }
          diceIndex++;
        }
      }
    }
    final double expectedHits = ((double) totalPower) / data.getDiceSides();
    final DiceRoll diceRoll = new DiceRoll(dice, hitCount, expectedHits);
    bridge.getHistoryWriter().addChildToEvent(annotation + " : " + MyFormatter.asDice(random), diceRoll);
    return diceRoll;
  }

  private static boolean isFirstTurnLimitedRoll(final PlayerID player, final GameData data) {
    // If player is null, Round > 1, or player has negate rule set: return false
    if (player.isNull() || (data.getSequence().getRound() != 1) || isNegateDominatingFirstRoundAttack(player)) {
      return false;
    }
    return isDominatingFirstRoundAttack(data.getSequence().getStep().getPlayerId());
  }

  private static boolean isDominatingFirstRoundAttack(final PlayerID player) {
    if (player == null) {
      return false;
    }
    final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
    if (ra == null) {
      return false;
    }
    return ra.getDominatingFirstRoundAttack();
  }

  private static boolean isNegateDominatingFirstRoundAttack(final PlayerID player) {
    final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
    if (ra == null) {
      return false;
    }
    return ra.getNegateDominatingFirstRoundAttack();
  }

  static String getAnnotation(final List<Unit> units, final PlayerID player, final IBattle battle) {
    final StringBuilder buffer = new StringBuilder(80);
    buffer.append(player.getName()).append(" roll dice for ").append(MyFormatter.unitsToTextNoOwner(units));
    if (battle != null) {
      buffer.append(" in ").append(battle.getTerritory().getName()).append(", round ")
          .append((battle.getBattleRound() + 1));
    }
    return buffer.toString();
  }

  /**
   * @param dice
   *        int[] the dice, 0 based
   * @param hits
   *        int - the number of hits
   * @param rollAt
   *        int - what we roll at, [0,Constants.MAX_DICE]
   * @param hitOnlyIfEquals
   *        boolean - do we get a hit only if we are equals, or do we hit
   *        when we are equal or less than for example a 5 is a hit when
   *        rolling at 6 for equal and less than, but is not for equals
   */
  public DiceRoll(final int[] dice, final int hits, final int rollAt, final boolean hitOnlyIfEquals) {
    this.hits = hits;
    expectedHits = 0;
    rolls = new ArrayList<>(dice.length);
    for (final int element : dice) {
      final boolean hit;
      if (hitOnlyIfEquals) {
        hit = (rollAt == element);
      } else {
        hit = element <= rollAt;
      }
      rolls.add(new Die(element, rollAt, hit ? DieType.HIT : DieType.MISS));
    }
  }

  // only for externalizable
  public DiceRoll() {}

  private DiceRoll(final List<Die> dice, final int hits, final double expectedHits) {
    rolls = new ArrayList<>(dice);
    this.hits = hits;
    this.expectedHits = expectedHits;
  }

  public int getHits() {
    return hits;
  }

  public double getExpectedHits() {
    return expectedHits;
  }

  /**
   * @param rollAt
   *        the strength of the roll, eg infantry roll at 2, expecting a
   *        number in [1,6]
   * @return in int[] which shouldnt be modifed, the int[] is 0 based, ie
   *         0..MAX_DICE
   */
  public List<Die> getRolls(final int rollAt) {
    final List<Die> dice = new ArrayList<>();
    for (final Die die : rolls) {
      if (die.getRolledAt() == rollAt) {
        dice.add(die);
      }
    }
    return dice;
  }

  public int size() {
    return rolls.size();
  }

  public Die getDie(final int index) {
    return rolls.get(index);
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    final int[] dice = new int[rolls.size()];
    for (int i = 0; i < rolls.size(); i++) {
      dice[i] = rolls.get(i).getCompressedValue();
    }
    out.writeObject(dice);
    out.writeInt(hits);
    out.writeDouble(expectedHits);
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    final int[] dice = (int[]) in.readObject();
    rolls = new ArrayList<>(dice.length);
    for (final int element : dice) {
      rolls.add(Die.getFromWriteValue(element));
    }
    hits = in.readInt();
    try {
      expectedHits = in.readDouble();
    } catch (final EOFException e) {
      // TODO: Ignore, can remove exception handling on incompatible release
    }
  }

  @Override
  public String toString() {
    return "DiceRoll dice:" + rolls + " hits:" + hits + " expectedHits:" + expectedHits;
  }
}
