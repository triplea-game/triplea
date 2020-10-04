package games.strategy.triplea.delegate.dice;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.power.calculator.AvailableSupportCalculator;
import games.strategy.triplea.delegate.power.calculator.SupportBonusCalculator;
import games.strategy.triplea.delegate.power.calculator.SupportCalculationResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Triple;
import org.triplea.util.Tuple;

@Value
@Builder(access = AccessLevel.PRIVATE)
public class TotalPowerAndTotalRolls {
  @Nonnull Integer totalPower;
  @Nonnull Integer totalRolls;

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
          sortedDice.add(new Die(dice[i], hitAt, hit ? Die.DieType.HIT : Die.DieType.MISS));
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
        sortedDice.add(
            new Die(dice[i], hitAtForInfinite, hit ? Die.DieType.HIT : Die.DieType.MISS));
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
          sortedDice.add(new Die(dice[i], hitAt, hit ? Die.DieType.HIT : Die.DieType.MISS));
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
  public static void sortAaHighToLow(
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
          builder() //
              .totalPower(strength)
              .totalRolls(rolls)
              .build());
    }

    return unitPowerAndRolls;
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

      unitPowerAndRolls.put(unit, builder().totalPower(strength).totalRolls(rolls).build());
    }

    return unitPowerAndRolls;
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

  public static int getTotalPower(
      final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap, final GameData data) {
    return getTotalPowerAndRolls(unitPowerAndRollsMap, data).getTotalPower();
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
    for (final Map.Entry<Unit, TotalPowerAndTotalRolls> entry : unitPowerAndRollsMap.entrySet()) {
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

    return builder().totalPower(totalPower).totalRolls(totalRolls).build();
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
    for (final Map.Entry<Unit, TotalPowerAndTotalRolls> entry : unitPowerAndRollsMap.entrySet()) {
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
