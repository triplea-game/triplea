package games.strategy.triplea.delegate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
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
import games.strategy.triplea.delegate.battle.AirBattle;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.power.calculator.AvailableSupportCalculator;
import games.strategy.triplea.delegate.power.calculator.SupportBonusCalculator;
import games.strategy.triplea.delegate.power.calculator.SupportCalculationResult;
import games.strategy.triplea.formatter.MyFormatter;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Triple;
import org.triplea.util.Tuple;

/**
 * Used to store information about a dice roll.
 *
 * <p># of rolls at 5, at 4, etc.
 *
 * <p>Externalizable so we can efficiently write out our dice as ints rather than as full objects.
 */
public class DiceRoll implements Externalizable {
  private static final long serialVersionUID = -1167204061937566271L;
  private List<Die> rolls;
  // this does not need to match the Die with isHit true since for low luck we get many hits with
  // few dice
  private int hits;
  private double expectedHits;

  /**
   * Initializes a new instance of the DiceRoll class.
   *
   * @param dice the dice, 0 based
   * @param hits the number of hits
   * @param rollAt what we roll at, [0,Constants.MAX_DICE]
   * @param hitOnlyIfEquals Do we get a hit only if we are equals, or do we hit when we are equal or
   *     less than for example a 5 is a hit when rolling at 6 for equal and less than, but is not
   *     for equals.
   */
  public DiceRoll(
      final int[] dice, final int hits, final int rollAt, final boolean hitOnlyIfEquals) {
    this.hits = hits;
    expectedHits = 0;
    rolls =
        Arrays.stream(dice)
            .mapToObj(
                element -> {
                  final boolean hit = hitOnlyIfEquals ? (rollAt == element) : element <= rollAt;
                  return new Die(element, rollAt, hit ? DieType.HIT : DieType.MISS);
                })
            .collect(Collectors.toList());
  }

  // only for externalizable
  public DiceRoll() {}

  private DiceRoll(final List<Die> dice, final int hits, final double expectedHits) {
    rolls = new ArrayList<>(dice);
    this.hits = hits;
    this.expectedHits = expectedHits;
  }

  public static Tuple<Integer, Integer> getMaxAaAttackAndDiceSides(
      final Collection<Unit> aaUnits, final GameData data, final boolean defending) {
    return getMaxAaAttackAndDiceSides(aaUnits, data, defending, new HashMap<>());
  }

  /**
   * Returns a Tuple, the first is the max attack, the second is the max dice sides for the AA unit
   * with that attack value.
   */
  public static Tuple<Integer, Integer> getMaxAaAttackAndDiceSides(
      final Collection<Unit> aaUnits,
      final GameData data,
      final boolean defending,
      final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap) {
    int highestAttack = 0;
    final int diceSize = data.getDiceSides();
    int chosenDiceSize = diceSize;
    for (final Unit u : aaUnits) {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      int uaDiceSides =
          defending ? ua.getAttackAaMaxDieSides() : ua.getOffensiveAttackAaMaxDieSides();
      if (uaDiceSides < 1) {
        uaDiceSides = diceSize;
      }
      int attack = defending ? ua.getAttackAa(u.getOwner()) : ua.getOffensiveAttackAa(u.getOwner());
      if (unitPowerAndRollsMap.containsKey(u)) {
        attack = unitPowerAndRollsMap.get(u).getTotalPower();
      }
      if (attack > uaDiceSides) {
        attack = uaDiceSides;
      }
      if ((((float) attack) / ((float) uaDiceSides))
          > (((float) highestAttack) / ((float) chosenDiceSize))) {
        highestAttack = attack;
        chosenDiceSize = uaDiceSides;
      }
    }

    return Tuple.of(highestAttack, chosenDiceSize);
  }

  /**
   * Finds total number of AA attacks that a group of units can roll against targets taking into
   * account infinite roll and overstack AA.
   */
  public static int getTotalAaAttacks(
      final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap,
      final Collection<Unit> validTargets) {
    if (unitPowerAndRollsMap.isEmpty() || validTargets.isEmpty()) {
      return 0;
    }
    int totalAAattacksNormal = 0;
    int totalAAattacksSurplus = 0;
    for (final Entry<Unit, TotalPowerAndTotalRolls> entry : unitPowerAndRollsMap.entrySet()) {
      if (entry.getValue().getTotalPower() == 0 || entry.getValue().getTotalRolls() == 0) {
        continue;
      }
      final UnitAttachment ua = UnitAttachment.get(entry.getKey().getType());
      if (entry.getValue().getTotalRolls() == -1) {
        totalAAattacksNormal = validTargets.size();
      } else {
        if (ua.getMayOverStackAa()) {
          totalAAattacksSurplus += entry.getValue().getTotalRolls();
        } else {
          totalAAattacksNormal += entry.getValue().getTotalRolls();
        }
      }
    }
    totalAAattacksNormal = Math.min(totalAAattacksNormal, validTargets.size());
    return totalAAattacksNormal + totalAAattacksSurplus;
  }

  /** Used only for rolling SBR or fly over AA as they don't currently take into account support. */
  public static DiceRoll rollSbrOrFlyOverAa(
      final Collection<Unit> validTargets,
      final Collection<Unit> aaUnits,
      final IDelegateBridge bridge,
      final Territory location,
      final boolean defending) {
    return rollAa(
        validTargets, aaUnits, new ArrayList<>(), new ArrayList<>(), bridge, location, defending);
  }

  /**
   * Used to roll AA for battles, SBR, and fly over.
   *
   * @param validTargets - potential AA targets
   * @param aaUnits - AA units that could potentially be rolling
   * @param allEnemyUnitsAliveOrWaitingToDie - all enemy units to check for support
   * @param allFriendlyUnitsAliveOrWaitingToDie - all allied units to check for support
   * @param bridge - delegate bridge
   * @param location - battle territory
   * @param defending - whether AA units are defending or attacking
   * @return DiceRoll result which includes total hits and dice that were rolled
   */
  public static DiceRoll rollAa(
      final Collection<Unit> validTargets,
      final Collection<Unit> aaUnits,
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final IDelegateBridge bridge,
      final Territory location,
      final boolean defending) {

    final GameData data = bridge.getData();
    final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap =
        getAaUnitPowerAndRollsForNormalBattles(
            aaUnits,
            allEnemyUnitsAliveOrWaitingToDie,
            allFriendlyUnitsAliveOrWaitingToDie,
            defending,
            data);

    // Check that there are valid AA and targets to roll for
    final int totalAaAttacks = getTotalAaAttacks(unitPowerAndRollsMap, validTargets);
    if (totalAaAttacks <= 0) {
      return new DiceRoll(List.of(), 0, 0);
    }

    // Determine dice sides (doesn't handle the possibility of different dice sides within the same
    // typeAA)
    final int diceSides =
        getMaxAaAttackAndDiceSides(aaUnits, data, defending, unitPowerAndRollsMap).getSecond();

    // Roll AA dice for LL or regular
    int hits = 0;
    final List<Die> sortedDice = new ArrayList<>();
    final String typeAa = UnitAttachment.get(aaUnits.iterator().next().getType()).getTypeAa();
    final int totalPower =
        getTotalAaPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(
                null, null, defending, unitPowerAndRollsMap, validTargets, data, false)
            .getFirst();
    final GamePlayer player = aaUnits.iterator().next().getOwner();
    final String annotation = "Roll " + typeAa + " in " + location.getName();
    if (Properties.getLowLuck(data) || Properties.getLowLuckAaOnly(data)) {
      hits += getAaLowLuckHits(bridge, sortedDice, totalPower, diceSides, player, annotation);
    } else {
      final int[] dice =
          bridge.getRandom(diceSides, totalAaAttacks, player, DiceType.COMBAT, annotation);
      hits +=
          getTotalAaPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(
                  dice, sortedDice, defending, unitPowerAndRollsMap, validTargets, data, true)
              .getSecond();
    }

    // Add dice results to history
    final double expectedHits = ((double) totalPower) / diceSides;
    final DiceRoll roll = new DiceRoll(sortedDice, hits, expectedHits);
    final String historyMessage =
        player.getName()
            + " roll "
            + typeAa
            + " dice in "
            + location
            + " : "
            + MyFormatter.asDice(roll);
    bridge.getHistoryWriter().addChildToEvent(historyMessage, roll);

    return roll;
  }

  /**
   * Single method for both LL and Dice, because if we have 2 methods then there is a chance they
   * will go out of sync. <br>
   * <br>
   * The following is complex, but should do the following:
   *
   * <ol>
   *   <li>Any aa that are NOT infinite attacks, and NOT overstack, will fire first individually
   *       ((because their power/dicesides might be different [example: radar tech on a german aa
   *       gun, in the same territory as an italian aagun without radar, neither is infinite])
   *   <li>All aa that have "infinite attacks" will have the one with the highest power/dicesides of
   *       them all, fire at whatever aa units have not yet been fired at. HOWEVER, if the
   *       non-infinite attackers are less powerful than the infinite attacker, then the
   *       non-infinite will not fire, and the infinite one will do all the attacks for both groups.
   *   <li>The total number of shots from these first 2 groups cannot exceed the number of air units
   *       being shot at
   *   <li>Any aa that can overstack will fire after, individually (aa guns that is both infinite,
   *       and overstacks, ignores the overstack part because that totally doesn't make any sense)
   * </ol>
   *
   * @param dice Rolled Dice numbers from bridge. Can be null if we do not want to return hits or
   *     fill the sortedDice
   * @param sortedDice List of dice we are recording. Can be null if we do not want to return hits
   *     or fill the sortedDice
   * @return An object containing 3 things: first is the total power of the aaUnits who will be
   *     rolling, second is number of hits, third is true/false are all rolls using the same hitAt
   *     (example: if all the rolls are at 1, we would return true, but if one roll is at 1 and
   *     another roll is at 2, then we return false)
   */
  public static Triple<Integer, Integer, Boolean>
      getTotalAaPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(
          final int[] dice,
          final List<Die> sortedDice,
          final boolean defending,
          final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap,
          final Collection<Unit> validTargets,
          final GameData data,
          final boolean fillInSortedDiceAndRecordHits) {

    // Check that there are valid AA and targets to roll for
    if (unitPowerAndRollsMap.isEmpty()) {
      return Triple.of(0, 0, false);
    }

    // Make sure the higher powers fire
    final List<Unit> aaToRoll = new ArrayList<>(unitPowerAndRollsMap.keySet());
    sortAaHighToLow(aaToRoll, data, defending, unitPowerAndRollsMap);

    // Setup all 3 groups of aa guns
    final List<Unit> normalNonInfiniteAa = new ArrayList<>(aaToRoll);
    final List<Unit> infiniteAa =
        CollectionUtils.getMatches(aaToRoll, Matches.unitMaxAaAttacksIsInfinite());
    final List<Unit> overstackAa =
        CollectionUtils.getMatches(aaToRoll, Matches.unitMayOverStackAa());
    overstackAa.removeAll(infiniteAa);
    normalNonInfiniteAa.removeAll(infiniteAa);
    normalNonInfiniteAa.removeAll(overstackAa);

    // Determine maximum total attacks
    final int totalAAattacksTotal = getTotalAaAttacks(unitPowerAndRollsMap, validTargets);

    // Determine individual totals
    final Map<Unit, TotalPowerAndTotalRolls> normalNonInfiniteAaMap =
        new HashMap<>(unitPowerAndRollsMap);
    normalNonInfiniteAaMap.keySet().retainAll(normalNonInfiniteAa);
    final int normalNonInfiniteAAtotalAAattacks =
        getTotalAaAttacks(normalNonInfiniteAaMap, validTargets);
    final Map<Unit, TotalPowerAndTotalRolls> infiniteAaMap = new HashMap<>(unitPowerAndRollsMap);
    infiniteAaMap.keySet().retainAll(infiniteAa);
    final int infiniteAAtotalAAattacks =
        Math.min(
            (validTargets.size() - normalNonInfiniteAAtotalAAattacks),
            getTotalAaAttacks(infiniteAaMap, validTargets));
    final Map<Unit, TotalPowerAndTotalRolls> overstackAaMap = new HashMap<>(unitPowerAndRollsMap);
    overstackAaMap.keySet().retainAll(overstackAa);
    final int overstackAAtotalAAattacks = getTotalAaAttacks(overstackAaMap, validTargets);
    if (totalAAattacksTotal
        != (normalNonInfiniteAAtotalAAattacks
            + infiniteAAtotalAAattacks
            + overstackAAtotalAAattacks)) {
      throw new IllegalStateException(
          "Total attacks should be: "
              + totalAAattacksTotal
              + " but instead is: "
              + (normalNonInfiniteAAtotalAAattacks
                  + infiniteAAtotalAAattacks
                  + overstackAAtotalAAattacks));
    }

    // Determine highest attack for infinite group
    final int hitAtForInfinite =
        getMaxAaAttackAndDiceSides(infiniteAa, data, defending, unitPowerAndRollsMap).getFirst();

    // If LL, the power and total attacks, else if dice we will be filling the sorted dice
    final boolean recordSortedDice =
        fillInSortedDiceAndRecordHits && dice != null && dice.length > 0 && sortedDice != null;
    int totalPower = 0;
    int hits = 0;
    int i = 0;
    final Set<Integer> rolledAt = new HashSet<>();

    // Non-infinite, non-overstack aa
    int runningMaximum = normalNonInfiniteAAtotalAAattacks;
    final Iterator<Unit> normalAAiter = normalNonInfiniteAa.iterator();
    while (i < runningMaximum && normalAAiter.hasNext()) {
      final Unit aaGun = normalAAiter.next();
      int numAttacks = unitPowerAndRollsMap.get(aaGun).getTotalRolls();
      final int hitAt = unitPowerAndRollsMap.get(aaGun).getTotalPower();
      if (hitAt < hitAtForInfinite) {
        continue;
      }
      while (i < runningMaximum && numAttacks > 0) {
        if (recordSortedDice) {
          // Dice are zero based
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

    // Infinite aa
    runningMaximum += infiniteAAtotalAAattacks;
    while (i < runningMaximum) {
      // Use the highest attack of this group, since each is infinite. (this is the default behavior
      // in revised)
      if (recordSortedDice) {
        // Dice are zero based
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

    // Overstack aa
    runningMaximum += overstackAAtotalAAattacks;
    final Iterator<Unit> overstackAAiter = overstackAa.iterator();
    while (i < runningMaximum && overstackAAiter.hasNext()) {
      final Unit aaGun = overstackAAiter.next();
      int numAttacks = unitPowerAndRollsMap.get(aaGun).getTotalRolls();
      final int hitAt = unitPowerAndRollsMap.get(aaGun).getTotalPower();
      while (i < runningMaximum && numAttacks > 0) {
        if (recordSortedDice) {
          // Dice are zero based
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

  private static void sortAaHighToLow(
      final List<Unit> units, final GameData data, final boolean defending) {
    sortAaHighToLow(units, data, defending, new HashMap<>());
  }

  @VisibleForTesting
  static void sortAaHighToLow(
      final List<Unit> units,
      final GameData data,
      final boolean defending,
      final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap) {
    units.sort(
        Comparator.comparing(
            unit -> getMaxAaAttackAndDiceSides(Set.of(unit), data, defending, unitPowerAndRollsMap),
            Comparator.<Tuple<Integer, Integer>, Boolean>comparing(tuple -> tuple.getFirst() == 0)
                .thenComparingDouble(tuple -> -tuple.getFirst() / (float) tuple.getSecond())));
  }

  /**
   * Returns the AA power (strength) and rolls for each of the AA units in the specified list. The
   * power is either attackAA or offensiveAttackAA plus any support. The rolls is maxAAattacks plus
   * any support if it isn't infinite (-1).
   *
   * @param aaUnits should be sorted from weakest to strongest, before the method is called, for the
   *     actual battle.
   */
  public static Map<Unit, TotalPowerAndTotalRolls> getAaUnitPowerAndRollsForNormalBattles(
      final Collection<Unit> aaUnits,
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final boolean defending,
      final GameData data) {

    final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRolls = new HashMap<>();
    if (aaUnits == null || aaUnits.isEmpty()) {
      return unitPowerAndRolls;
    }

    // Get all friendly supports
    final SupportCalculationResult friendlySupports =
        AvailableSupportCalculator.getSortedAaSupport(
            allFriendlyUnitsAliveOrWaitingToDie, //
            data,
            defending,
            true);
    final Set<List<UnitSupportAttachment>> supportRulesFriendly =
        friendlySupports.getSupportRules();
    final IntegerMap<UnitSupportAttachment> supportLeftFriendly = friendlySupports.getSupportLeft();
    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnitsLeftFriendly =
        friendlySupports.getSupportUnits();

    // Get all enemy supports
    final SupportCalculationResult enemySupports =
        AvailableSupportCalculator.getSortedAaSupport(
            allEnemyUnitsAliveOrWaitingToDie, //
            data,
            !defending,
            false);

    final Set<List<UnitSupportAttachment>> supportRulesEnemy = enemySupports.getSupportRules();
    final IntegerMap<UnitSupportAttachment> supportLeftEnemy = enemySupports.getSupportLeft();
    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnitsLeftEnemy =
        enemySupports.getSupportUnits();

    // Copy for rolls
    final IntegerMap<UnitSupportAttachment> supportLeftFriendlyRolls =
        new IntegerMap<>(supportLeftFriendly);
    final IntegerMap<UnitSupportAttachment> supportLeftEnemyRolls =
        new IntegerMap<>(supportLeftEnemy);
    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnitsLeftFriendlyRolls =
        new HashMap<>();
    for (final UnitSupportAttachment usa : supportUnitsLeftFriendly.keySet()) {
      supportUnitsLeftFriendlyRolls.put(usa, new IntegerMap<>(supportUnitsLeftFriendly.get(usa)));
    }
    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnitsLeftEnemyRolls = new HashMap<>();
    for (final UnitSupportAttachment usa : supportUnitsLeftEnemy.keySet()) {
      supportUnitsLeftEnemyRolls.put(usa, new IntegerMap<>(supportUnitsLeftEnemy.get(usa)));
    }

    // Sort units strongest to weakest to give support to the best units first
    final List<Unit> sortedAaUnits = new ArrayList<>(aaUnits);
    sortAaHighToLow(sortedAaUnits, data, defending);
    for (final Unit unit : sortedAaUnits) {

      // Find unit's AA strength
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      int strength =
          defending ? ua.getAttackAa(unit.getOwner()) : ua.getOffensiveAttackAa(unit.getOwner());
      strength +=
          SupportBonusCalculator.getSupport(
              unit,
              supportRulesFriendly,
              supportLeftFriendly,
              supportUnitsLeftFriendly,
              new HashMap<>(),
              UnitSupportAttachment::getAaStrength);
      strength +=
          SupportBonusCalculator.getSupport(
              unit,
              supportRulesEnemy,
              supportLeftEnemy,
              supportUnitsLeftEnemy,
              new HashMap<>(),
              UnitSupportAttachment::getAaStrength);
      strength = Math.min(Math.max(strength, 0), data.getDiceSides());

      // Find unit's AA rolls
      int rolls;
      if (strength == 0) {
        rolls = 0;
      } else {
        rolls = ua.getMaxAaAttacks();
        if (rolls > -1) {
          rolls +=
              SupportBonusCalculator.getSupport(
                  unit,
                  supportRulesFriendly,
                  supportLeftFriendlyRolls,
                  supportUnitsLeftFriendlyRolls,
                  new HashMap<>(),
                  UnitSupportAttachment::getAaRoll);
          rolls +=
              SupportBonusCalculator.getSupport(
                  unit,
                  supportRulesEnemy,
                  supportLeftEnemyRolls,
                  supportUnitsLeftEnemyRolls,
                  new HashMap<>(),
                  UnitSupportAttachment::getAaRoll);
          rolls = Math.max(0, rolls);
        }
        if (rolls == 0) {
          strength = 0;
        }
      }

      unitPowerAndRolls.put(
          unit, //
          TotalPowerAndTotalRolls.builder() //
              .totalPower(strength)
              .totalRolls(rolls)
              .build());
    }

    return unitPowerAndRolls;
  }

  private static int getAaLowLuckHits(
      final IDelegateBridge bridge,
      final List<Die> sortedDice,
      final int totalPower,
      final int chosenDiceSize,
      final GamePlayer playerRolling,
      final String annotation) {
    int hits = totalPower / chosenDiceSize;
    final int hitsFractional = totalPower % chosenDiceSize;
    if (hitsFractional > 0) {
      final int[] dice =
          bridge.getRandom(chosenDiceSize, 1, playerRolling, DiceType.COMBAT, annotation);
      final boolean hit = hitsFractional > dice[0];
      if (hit) {
        hits++;
      }
      final Die die = new Die(dice[0], hitsFractional, hit ? DieType.HIT : DieType.MISS);
      sortedDice.add(die);
    }
    return hits;
  }

  @VisibleForTesting
  static DiceRoll rollDice(
      final List<Unit> units,
      final boolean defending,
      final GamePlayer player,
      final IDelegateBridge bridge,
      final IBattle battle,
      final Collection<TerritoryEffect> territoryEffects) {
    return rollDice(
        units, defending, player, bridge, battle, "", territoryEffects, List.of(), units);
  }

  /**
   * Used to roll dice for attackers and defenders in battles.
   *
   * @param units - units that could potentially be rolling
   * @param defending - whether units are defending or attacking
   * @param player - that will be rolling the dice
   * @param bridge - delegate bridge
   * @param battle - which the dice are being rolled for
   * @param annotation - description of the battle being rolled for
   * @param territoryEffects - list of territory effects for the battle
   * @param allEnemyUnitsAliveOrWaitingToDie - all enemy units to check for support
   * @param allFriendlyUnitsAliveOrWaitingToDie - all allied units to check for support
   * @return DiceRoll result which includes total hits and dice that were rolled
   */
  public static DiceRoll rollDice(
      final List<Unit> units,
      final boolean defending,
      final GamePlayer player,
      final IDelegateBridge bridge,
      final IBattle battle,
      final String annotation,
      final Collection<TerritoryEffect> territoryEffects,
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie) {

    if (Properties.getLowLuck(bridge.getData())) {
      return rollDiceLowLuck(
          units,
          defending,
          player,
          bridge,
          battle,
          annotation,
          territoryEffects,
          allEnemyUnitsAliveOrWaitingToDie,
          allFriendlyUnitsAliveOrWaitingToDie);
    }
    return rollDiceNormal(
        units,
        defending,
        player,
        bridge,
        battle,
        annotation,
        territoryEffects,
        allEnemyUnitsAliveOrWaitingToDie,
        allFriendlyUnitsAliveOrWaitingToDie);
  }

  /**
   * Roll n-sided dice.
   *
   * @param annotation 0 based, add 1 to get actual die roll
   */
  public static DiceRoll rollNDice(
      final IDelegateBridge bridge,
      final int rollCount,
      final int sides,
      final GamePlayer playerRolling,
      final DiceType diceType,
      final String annotation) {
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
   * Returns the power (strength) and rolls for each of the specified units.
   *
   * @param unitsGettingPowerFor should be sorted from weakest to strongest, before the method is
   *     called, for the actual battle.
   */
  public static Map<Unit, TotalPowerAndTotalRolls> getUnitPowerAndRollsForNormalBattles(
      final Collection<Unit> unitsGettingPowerFor,
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final boolean defending,
      final GameData data,
      final Territory location,
      final Collection<TerritoryEffect> territoryEffects) {

    return getUnitPowerAndRollsForNormalBattles(
        unitsGettingPowerFor,
        allEnemyUnitsAliveOrWaitingToDie,
        allFriendlyUnitsAliveOrWaitingToDie,
        defending,
        data,
        location,
        territoryEffects,
        new HashMap<>(),
        new HashMap<>());
  }

  /**
   * Returns the power (strength) and rolls for each of the specified units.
   *
   * @param unitsGettingPowerFor should be sorted from weakest to strongest, before the method is
   *     called, for the actual battle.
   */
  public static Map<Unit, TotalPowerAndTotalRolls> getUnitPowerAndRollsForNormalBattles(
      final Collection<Unit> unitsGettingPowerFor,
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final boolean defending,
      final GameData data,
      final Territory location,
      final Collection<TerritoryEffect> territoryEffects,
      final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap,
      final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap) {

    final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRolls = new HashMap<>();
    if (unitsGettingPowerFor == null || unitsGettingPowerFor.isEmpty()) {
      return unitPowerAndRolls;
    }

    // Get all friendly supports
    final SupportCalculationResult friendlySupport =
        AvailableSupportCalculator.getSortedSupport(
            allFriendlyUnitsAliveOrWaitingToDie,
            data.getUnitTypeList().getSupportRules(),
            defending,
            true);
    final Set<List<UnitSupportAttachment>> supportRulesFriendly = friendlySupport.getSupportRules();
    final IntegerMap<UnitSupportAttachment> supportLeftFriendly = friendlySupport.getSupportLeft();
    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnitsLeftFriendly =
        friendlySupport.getSupportUnits();

    // Get all enemy supports
    final SupportCalculationResult enemySupport =
        AvailableSupportCalculator.getSortedSupport(
            allEnemyUnitsAliveOrWaitingToDie,
            data.getUnitTypeList().getSupportRules(),
            !defending,
            false);
    final Set<List<UnitSupportAttachment>> supportRulesEnemy = enemySupport.getSupportRules();
    final IntegerMap<UnitSupportAttachment> supportLeftEnemy = enemySupport.getSupportLeft();
    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnitsLeftEnemy =
        enemySupport.getSupportUnits();

    // Copy for rolls
    final IntegerMap<UnitSupportAttachment> supportLeftFriendlyRolls =
        new IntegerMap<>(supportLeftFriendly);
    final IntegerMap<UnitSupportAttachment> supportLeftEnemyRolls =
        new IntegerMap<>(supportLeftEnemy);
    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnitsLeftFriendlyRolls =
        new HashMap<>();
    for (final UnitSupportAttachment usa : supportUnitsLeftFriendly.keySet()) {
      supportUnitsLeftFriendlyRolls.put(usa, new IntegerMap<>(supportUnitsLeftFriendly.get(usa)));
    }
    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnitsLeftEnemyRolls = new HashMap<>();
    for (final UnitSupportAttachment usa : supportUnitsLeftEnemy.keySet()) {
      supportUnitsLeftEnemyRolls.put(usa, new IntegerMap<>(supportUnitsLeftEnemy.get(usa)));
    }

    for (final Unit unit : unitsGettingPowerFor) {

      // Find unit's strength
      int strength;
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (defending) {
        strength = ua.getDefense(unit.getOwner());
        if (isFirstTurnLimitedRoll(unit.getOwner(), data)) {
          strength = Math.min(1, strength);
        } else {
          strength +=
              SupportBonusCalculator.getSupport(
                  unit,
                  supportRulesFriendly,
                  supportLeftFriendly,
                  supportUnitsLeftFriendly,
                  unitSupportPowerMap,
                  UnitSupportAttachment::getStrength);
        }
        strength +=
            SupportBonusCalculator.getSupport(
                unit,
                supportRulesEnemy,
                supportLeftEnemy,
                supportUnitsLeftEnemy,
                unitSupportPowerMap,
                UnitSupportAttachment::getStrength);
      } else {
        strength = ua.getAttack(unit.getOwner());
        if (ua.getIsMarine() != 0 && unit.getWasAmphibious()) {
          strength += ua.getIsMarine();
        }
        if (ua.getIsSea() && Matches.territoryIsLand().test(location)) {
          // Change the strength to be bombard, not attack/defense, because this is a bombarding
          // naval unit
          strength = ua.getBombard();
        }
        strength +=
            SupportBonusCalculator.getSupport(
                unit,
                supportRulesFriendly,
                supportLeftFriendly,
                supportUnitsLeftFriendly,
                unitSupportPowerMap,
                UnitSupportAttachment::getStrength);
        strength +=
            SupportBonusCalculator.getSupport(
                unit,
                supportRulesEnemy,
                supportLeftEnemy,
                supportUnitsLeftEnemy,
                unitSupportPowerMap,
                UnitSupportAttachment::getStrength);
      }
      strength +=
          TerritoryEffectHelper.getTerritoryCombatBonus(
              unit.getType(), territoryEffects, defending);
      strength = Math.min(Math.max(strength, 0), data.getDiceSides());

      // Find unit's rolls
      int rolls;
      if (strength == 0) {
        rolls = 0;
      } else {
        if (defending) {
          rolls = ua.getDefenseRolls(unit.getOwner());
        } else {
          rolls = ua.getAttackRolls(unit.getOwner());
        }
        rolls +=
            SupportBonusCalculator.getSupport(
                unit,
                supportRulesFriendly,
                supportLeftFriendlyRolls,
                supportUnitsLeftFriendlyRolls,
                unitSupportRollsMap,
                UnitSupportAttachment::getRoll);
        rolls +=
            SupportBonusCalculator.getSupport(
                unit,
                supportRulesEnemy,
                supportLeftEnemyRolls,
                supportUnitsLeftEnemyRolls,
                unitSupportRollsMap,
                UnitSupportAttachment::getRoll);
        rolls = Math.max(0, rolls);
        if (rolls == 0) {
          strength = 0;
        }
      }

      unitPowerAndRolls.put(
          unit, TotalPowerAndTotalRolls.builder().totalPower(strength).totalRolls(rolls).build());
    }

    return unitPowerAndRolls;
  }

  public static int getTotalPower(
      final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap, final GameData data) {
    return getTotalPowerAndRolls(unitPowerAndRollsMap, data).getTotalPower();
  }

  @Value
  @Builder
  public static class TotalPowerAndTotalRolls {
    @Nonnull Integer totalPower;
    @Nonnull Integer totalRolls;

    /** Returns the product of power and dice rolls. */
    public int getEffectivePower() {
      return totalPower * totalRolls;
    }

    public TotalPowerAndTotalRolls subtractPower(final int powerToSubtract) {
      return TotalPowerAndTotalRolls.builder()
          .totalPower(totalPower - powerToSubtract)
          .totalRolls(totalRolls)
          .build();
    }

    public TotalPowerAndTotalRolls subtractRolls(final int rollsToSubtract) {
      return TotalPowerAndTotalRolls.builder()
          .totalPower(totalPower)
          .totalRolls(totalRolls - rollsToSubtract)
          .build();
    }
  }

  /**
   * Sums up for a given collection of units with power totals and rolls, a total power and total
   * rolls for all units.
   */
  public static TotalPowerAndTotalRolls getTotalPowerAndRolls(
      final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap, final GameData data) {

    final int diceSides = data.getDiceSides();
    final boolean lhtrBombers = Properties.getLhtrHeavyBombers(data);

    // Bonus is normally 1 for most games
    final int extraRollBonus = Math.max(1, data.getDiceSides() / 6);

    int totalPower = 0;
    int totalRolls = 0;
    for (final Entry<Unit, TotalPowerAndTotalRolls> entry : unitPowerAndRollsMap.entrySet()) {
      int unitStrength = Math.min(Math.max(0, entry.getValue().getTotalPower()), diceSides);
      final int unitRolls = entry.getValue().getTotalRolls();
      if (unitStrength <= 0 || unitRolls <= 0) {
        continue;
      }
      if (unitRolls == 1) {
        totalPower += unitStrength;
        totalRolls += unitRolls;
      } else {
        final UnitAttachment ua = UnitAttachment.get(entry.getKey().getType());
        if (lhtrBombers || ua.getChooseBestRoll()) {
          // LHTR means pick the best dice roll, which doesn't really make sense in LL. So instead,
          // we will just add +1 onto the power to simulate the gains of having the best die picked.
          unitStrength += extraRollBonus * (unitRolls - 1);
          totalPower += Math.min(unitStrength, diceSides);
          totalRolls += unitRolls;
        } else {
          totalPower += unitRolls * unitStrength;
          totalRolls += unitRolls;
        }
      }
    }

    return TotalPowerAndTotalRolls.builder().totalPower(totalPower).totalRolls(totalRolls).build();
  }

  /** Roll dice for units using low luck rules. */
  private static DiceRoll rollDiceLowLuck(
      final Collection<Unit> unitsList,
      final boolean defending,
      final GamePlayer player,
      final IDelegateBridge bridge,
      final IBattle battle,
      final String annotation,
      final Collection<TerritoryEffect> territoryEffects,
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie) {

    final List<Unit> units = new ArrayList<>(unitsList);
    final GameData data = bridge.getData();
    final Territory location = battle.getTerritory();
    final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap =
        DiceRoll.getUnitPowerAndRollsForNormalBattles(
            units,
            allEnemyUnitsAliveOrWaitingToDie,
            allFriendlyUnitsAliveOrWaitingToDie,
            defending,
            data,
            location,
            territoryEffects);

    final int power = getTotalPower(unitPowerAndRollsMap, data);
    if (power == 0) {
      return new DiceRoll(List.of(), 0, 0);
    }

    // Roll dice for the fractional part of the dice
    int hitCount = power / data.getDiceSides();
    final List<Die> dice = new ArrayList<>();
    final int rollFor = power % data.getDiceSides();
    final int[] random;
    if (rollFor == 0) {
      random = new int[0];
    } else {
      random = bridge.getRandom(data.getDiceSides(), 1, player, DiceType.COMBAT, annotation);
      // Zero based
      final boolean hit = rollFor > random[0];
      if (hit) {
        hitCount++;
      }
      dice.add(new Die(random[0], rollFor, hit ? DieType.HIT : DieType.MISS));
    }

    // Create DiceRoll object
    final double expectedHits = ((double) power) / data.getDiceSides();
    final DiceRoll diceRoll = new DiceRoll(dice, hitCount, expectedHits);
    bridge
        .getHistoryWriter()
        .addChildToEvent(annotation + " : " + MyFormatter.asDice(random), diceRoll);

    return diceRoll;
  }

  /**
   * Sorts the specified collection of units in ascending order of their attack or defense strength.
   *
   * @param defending {@code true} if the units should be sorted by their defense strength;
   *     otherwise the units will be sorted by their attack strength.
   */
  public static void sortByStrength(final List<Unit> units, final boolean defending) {
    // Pre-compute unit strength information to speed up the sort.
    final Table<UnitType, GamePlayer, Integer> strengthTable = HashBasedTable.create();
    for (final Unit unit : units) {
      final UnitType type = unit.getType();
      final GamePlayer owner = unit.getOwner();
      if (!strengthTable.contains(type, owner)) {
        if (defending) {
          strengthTable.put(type, owner, UnitAttachment.get(type).getDefense(owner));
        } else {
          strengthTable.put(type, owner, UnitAttachment.get(type).getAttack(owner));
        }
      }
    }
    final Comparator<Unit> comp =
        (u1, u2) -> {
          final int v1 = strengthTable.get(u1.getType(), u1.getOwner());
          final int v2 = strengthTable.get(u2.getType(), u2.getOwner());
          return Integer.compare(v1, v2);
        };
    units.sort(comp);
  }

  public static DiceRoll airBattle(
      final Collection<Unit> unitsList,
      final boolean defending,
      final GamePlayer player,
      final IDelegateBridge bridge,
      final String annotation) {

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
      final int strength =
          Math.min(
              data.getDiceSides(),
              Math.max(
                  0,
                  (defending
                      ? ua.getAirDefense(current.getOwner())
                      : ua.getAirAttack(current.getOwner()))));
      for (int i = 0; i < rolls; i++) {
        // LHTR means pick the best dice roll, which doesn't really make sense in LL. So instead, we
        // will just add +1
        // onto the power to simulate the gains of having the best die picked.
        if (i > 1 && (lhtrBombers || ua.getChooseBestRoll())) {
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
      random =
          bridge.getRandom(data.getDiceSides(), rollCount, player, DiceType.COMBAT, annotation);
      int diceIndex = 0;
      for (final Unit current : units) {
        final UnitAttachment ua = UnitAttachment.get(current.getType());
        final int strength =
            Math.min(
                data.getDiceSides(),
                Math.max(
                    0,
                    (defending
                        ? ua.getAirDefense(current.getOwner())
                        : ua.getAirAttack(current.getOwner()))));
        final int rolls = AirBattle.getAirBattleRolls(current, defending);
        // lhtr heavy bombers take best of n dice for both attack and defense
        if (rolls > 1 && (lhtrBombers || ua.getChooseBestRoll())) {
          int minIndex = 0;
          int min = data.getDiceSides();
          for (int i = 0; i < rolls; i++) {
            if (random[diceIndex + i] < min) {
              min = random[diceIndex + i];
              minIndex = i;
            }
          }
          final boolean hit = strength > random[diceIndex + minIndex];
          dice.add(
              new Die(random[diceIndex + minIndex], strength, hit ? DieType.HIT : DieType.MISS));
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
    bridge
        .getHistoryWriter()
        .addChildToEvent(annotation + " : " + MyFormatter.asDice(random), diceRoll);
    return diceRoll;
  }

  /** Roll dice for units per normal rules. */
  private static DiceRoll rollDiceNormal(
      final Collection<Unit> unitsList,
      final boolean defending,
      final GamePlayer player,
      final IDelegateBridge bridge,
      final IBattle battle,
      final String annotation,
      final Collection<TerritoryEffect> territoryEffects,
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie) {

    final List<Unit> units = new ArrayList<>(unitsList);
    final GameData data = bridge.getData();
    sortByStrength(units, defending);
    final Territory location = battle.getTerritory();
    final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap =
        DiceRoll.getUnitPowerAndRollsForNormalBattles(
            units,
            allEnemyUnitsAliveOrWaitingToDie,
            allFriendlyUnitsAliveOrWaitingToDie,
            defending,
            data,
            location,
            territoryEffects);

    final TotalPowerAndTotalRolls totalPowerAndRolls =
        getTotalPowerAndRolls(unitPowerAndRollsMap, data);
    final int rollCount = totalPowerAndRolls.getTotalRolls();
    if (rollCount == 0) {
      return new DiceRoll(new ArrayList<>(), 0, 0);
    }

    final int[] random =
        bridge.getRandom(data.getDiceSides(), rollCount, player, DiceType.COMBAT, annotation);
    final boolean lhtrBombers = Properties.getLhtrHeavyBombers(data);
    final List<Die> dice = new ArrayList<>();
    int hitCount = 0;
    int diceIndex = 0;
    for (final Unit current : units) {
      final UnitAttachment ua = UnitAttachment.get(current.getType());
      final TotalPowerAndTotalRolls powerAndRolls = unitPowerAndRollsMap.get(current);
      final int strength = powerAndRolls.getTotalPower();
      final int rolls = powerAndRolls.getTotalRolls();
      // lhtr heavy bombers take best of n dice for both attack and defense
      if (rolls <= 0 || strength <= 0) {
        continue;
      }
      if (rolls > 1 && (lhtrBombers || ua.getChooseBestRoll())) {
        int smallestDieIndex = 0;
        int smallestDie = data.getDiceSides();
        for (int i = 0; i < rolls; i++) {
          if (random[diceIndex + i] < smallestDie) {
            smallestDie = random[diceIndex + i];
            smallestDieIndex = i;
          }
        }
        // Zero based
        final boolean hit = strength > random[diceIndex + smallestDieIndex];
        dice.add(
            new Die(
                random[diceIndex + smallestDieIndex], strength, hit ? DieType.HIT : DieType.MISS));
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
          if (diceIndex >= random.length) {
            break;
          }
          // Zero based
          final boolean hit = strength > random[diceIndex];
          dice.add(new Die(random[diceIndex], strength, hit ? DieType.HIT : DieType.MISS));
          if (hit) {
            hitCount++;
          }
          diceIndex++;
        }
      }
    }

    final int totalPower = totalPowerAndRolls.getTotalPower();
    final double expectedHits = ((double) totalPower) / data.getDiceSides();
    final DiceRoll diceRoll = new DiceRoll(dice, hitCount, expectedHits);
    bridge
        .getHistoryWriter()
        .addChildToEvent(annotation + " : " + MyFormatter.asDice(random), diceRoll);

    return diceRoll;
  }

  private static boolean isFirstTurnLimitedRoll(final GamePlayer player, final GameData data) {
    // If player is null, Round > 1, or player has negate rule set: return false
    return !player.isNull()
        && data.getSequence().getRound() == 1
        && !isNegateDominatingFirstRoundAttack(player)
        && isDominatingFirstRoundAttack(data.getSequence().getStep().getPlayerId());
  }

  private static boolean isDominatingFirstRoundAttack(final GamePlayer player) {
    if (player == null) {
      return false;
    }
    final RulesAttachment ra =
        (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
    return ra != null && ra.getDominatingFirstRoundAttack();
  }

  private static boolean isNegateDominatingFirstRoundAttack(final GamePlayer player) {
    final RulesAttachment ra =
        (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
    return ra != null && ra.getNegateDominatingFirstRoundAttack();
  }

  /**
   * Parses the player name from the given annotation that has been produced by getAnnotation().
   *
   * @param annotation The annotation string.
   * @return The player's name.
   */
  public static String getPlayerNameFromAnnotation(final String annotation) {
    // This parses the "Germans roll dice for " format produced by getAnnotation() below.
    return annotation.split(" ", 2)[0];
  }

  public static String getAnnotation(
      final Collection<Unit> units,
      final GamePlayer player,
      final Territory territory,
      final int battleRound) {
    final StringBuilder buffer = new StringBuilder(80);
    // Note: This pattern is parsed when loading saved games to restore dice stats to get the player
    // name via the
    // getPlayerNameFromAnnotation() function above. When changing this format, update
    // getPlayerNameFromAnnotation(),
    // preferably in a way that is backwards compatible (can parse previous formats too).
    buffer
        .append(player.getName())
        .append(" roll dice for ")
        .append(MyFormatter.unitsToTextNoOwner(units))
        .append(" in ")
        .append(territory.getName())
        .append(", round ")
        .append(battleRound + 1);
    return buffer.toString();
  }

  public int getHits() {
    return hits;
  }

  public double getExpectedHits() {
    return expectedHits;
  }

  /**
   * Returns all rolls that are equal to the specified value.
   *
   * @param rollAt the strength of the roll, eg infantry roll at 2, expecting a number in [1,6]
   */
  public List<Die> getRolls(final int rollAt) {
    return rolls.stream() //
        .filter(die -> die.getRolledAt() == rollAt)
        .collect(Collectors.toList());
  }

  public int size() {
    return rolls.size();
  }

  public boolean isEmpty() {
    return rolls.isEmpty();
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
    expectedHits = in.readDouble();
  }

  @Override
  public String toString() {
    return "DiceRoll dice:" + rolls + " hits:" + hits + " expectedHits:" + expectedHits;
  }
}
