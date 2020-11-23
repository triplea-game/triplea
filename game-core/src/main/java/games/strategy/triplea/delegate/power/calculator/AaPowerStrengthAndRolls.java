package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;

/**
 * Computes the total power, total strength, and total roll for a collection of AA units
 *
 * <p>The strength is either attackAA or offensiveAttackAA plus any support. The rolls is
 * maxAAattacks plus any support if it isn't infinite (-1). The power is the sum of all the strength
 * that is actually used by the AA to hit the targetCount.
 *
 * <p>All the AA units in the group should have the same dice sides (as per attackAaMaxDieSides and
 * offensiveAttackAaMaxDieSides). If they don't, then the dice sides from unit with the best
 * strength / dice sides will be used.
 */
@Value
@Getter(AccessLevel.NONE)
public class AaPowerStrengthAndRolls implements TotalPowerAndTotalRolls {

  @Getter int bestStrength;
  @Getter int bestDiceSides;

  CombatValue calculator;

  Map<Unit, UnitPowerStrengthAndRolls> totalStrengthAndTotalRollsByUnit = new HashMap<>();

  int targetCount;

  /**
   * Holds the aa units that actually have rolls
   *
   * <p>The map needs to be sorted so that the groups of AA units fire in the correct order.
   *
   * <p>See {@link AaPowerStrengthAndRolls#calculateActiveStrengthAndRolls()} for how this is
   * calculated
   */
  List<UnitPowerStrengthAndRolls> activeStrengthAndRolls;

  private AaPowerStrengthAndRolls(
      final Collection<Unit> units, final int targetCount, final CombatValue calculator) {
    this.calculator = calculator;
    this.targetCount = targetCount;
    addUnits(units);

    int highestStrength = 0;
    int chosenDiceSides = 100;
    for (final Map.Entry<Unit, UnitPowerStrengthAndRolls> unitStrengthAndRolls :
        totalStrengthAndTotalRollsByUnit.entrySet()) {
      final Unit u = unitStrengthAndRolls.getKey();

      final int diceSides = calculator.getDiceSides(u);
      final int strength = unitStrengthAndRolls.getValue().getStrength();
      if ((((float) strength) / ((float) diceSides))
          > (((float) highestStrength) / ((float) chosenDiceSides))) {
        highestStrength = strength;
        chosenDiceSides = diceSides;
      }
    }

    this.bestStrength = highestStrength;
    this.bestDiceSides = chosenDiceSides;

    this.activeStrengthAndRolls = calculateActiveStrengthAndRolls();
  }

  /**
   * Builds an AaPowerStrengthAndRolls that calculates the power, strength, and roll for each unit
   *
   * @param aaUnits does not need to be sorted
   * @param targetCount the number of targets the AA is firing at
   * @param calculator calculates the value of offense or defense
   */
  public static AaPowerStrengthAndRolls build(
      final Collection<Unit> aaUnits, final int targetCount, final CombatValue calculator) {

    if (aaUnits == null || aaUnits.isEmpty()) {
      return new AaPowerStrengthAndRolls(List.of(), targetCount, calculator);
    }
    // First, sort units strongest to weakest without support so that later, the support is given
    // to the best units first
    return new AaPowerStrengthAndRolls(
        aaUnits.stream().sorted(calculator.unitComparator()).collect(Collectors.toList()),
        targetCount,
        calculator);
  }

  private void addUnits(final Collection<Unit> units) {
    final StrengthCalculator strengthCalculator = calculator.getStrength();
    final RollCalculator rollCalculator = calculator.getRoll();
    final PowerCalculator powerCalculator = calculator.getPower();
    for (final Unit unit : units) {
      int strength = strengthCalculator.getStrength(unit).getValue();
      int rolls = rollCalculator.getRoll(unit).getValue();
      if (rolls == 0 || strength == 0) {
        strength = 0;
        rolls = 0;
      }
      totalStrengthAndTotalRollsByUnit.put(
          unit,
          UnitPowerStrengthAndRolls.builder()
              .strength(strength)
              .rolls(rolls)
              .power(powerCalculator.getValue(unit))
              .powerCalculator(powerCalculator)
              .diceSides(calculator.getDiceSides(unit))
              .chooseBestRoll(calculator.chooseBestRoll(unit))
              .build());
    }
  }

  /**
   * To determine which units have rolls, the following logic is used:
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
   */
  private List<UnitPowerStrengthAndRolls> calculateActiveStrengthAndRolls() {
    // Make sure the higher powers fire first
    final List<Unit> aaToRoll =
        totalStrengthAndTotalRollsByUnit.keySet().stream()
            .sorted(calculator.unitComparator())
            .collect(Collectors.toList());

    // Setup all 3 groups of aa guns
    final List<Unit> basicAa = new ArrayList<>(aaToRoll);
    final List<Unit> infiniteAa =
        CollectionUtils.getMatches(aaToRoll, Matches.unitMaxAaAttacksIsInfinite());
    final List<Unit> overstackAa =
        CollectionUtils.getMatches(aaToRoll, Matches.unitMayOverStackAa());
    overstackAa.removeAll(infiniteAa);
    basicAa.removeAll(infiniteAa);
    basicAa.removeAll(overstackAa);

    // Determine highest strength for infinite group
    // Limit to AA with the same best dice sides in case there are different dice sides. Multiple
    // dice sides within the same AA firing group isn't supported
    final Optional<Unit> bestInfiniteUnit =
        infiniteAa.stream()
            .filter(unit -> calculator.getDiceSides(unit) == bestDiceSides)
            .max(
                Comparator.comparingInt(
                    unit -> totalStrengthAndTotalRollsByUnit.get(unit).getStrength()));

    final int strengthOfBestInfiniteUnit =
        bestInfiniteUnit
            .map(unit -> totalStrengthAndTotalRollsByUnit.get(unit).getStrength())
            .orElse(0);

    final List<UnitPowerStrengthAndRolls> activeStrengthAndRolls = new ArrayList<>();
    // keep track of the units that will roll so we can set all the others to 0 roll/strength
    final Collection<Unit> activeUnits = new ArrayList<>();

    // Add the non infinite units that are stronger than the infinite units.
    // If there are more non infinite rolls than targets, then ensure that the last unit
    // only fires enough to hit the remaining targets
    int totalBasicRolls = 0;
    for (final Unit unit : basicAa) {
      if (totalStrengthAndTotalRollsByUnit.get(unit).getStrength() <= strengthOfBestInfiniteUnit) {
        continue;
      }
      activeUnits.add(unit);
      final int roll = totalStrengthAndTotalRollsByUnit.get(unit).getRolls();
      if (totalBasicRolls + roll >= targetCount) {
        final int weakestBasicAaRolls = targetCount - totalBasicRolls;
        totalBasicRolls += weakestBasicAaRolls;
        final UnitPowerStrengthAndRolls originalUnitData =
            totalStrengthAndTotalRollsByUnit.get(unit);
        final UnitPowerStrengthAndRolls weakestBasicUnitData =
            originalUnitData.updateRolls(weakestBasicAaRolls);
        activeStrengthAndRolls.add(weakestBasicUnitData);
        // update the weakest rolling basic unit with the actual number of rolls it has
        totalStrengthAndTotalRollsByUnit.put(unit, weakestBasicUnitData);
        break;
      }
      activeStrengthAndRolls.add(totalStrengthAndTotalRollsByUnit.get(unit));
      totalBasicRolls += roll;
    }

    // Add infinite AA if there are targets left for it
    final int totalBasicRollsFinal = totalBasicRolls;
    if (targetCount - totalBasicRollsFinal > 0) {
      final List<UnitPowerStrengthAndRolls> infinitePowerStrengthAndRolls = new ArrayList<>();
      bestInfiniteUnit.ifPresent(
          unit -> {
            final UnitPowerStrengthAndRolls originalUnitData =
                totalStrengthAndTotalRollsByUnit.get(unit);
            final UnitPowerStrengthAndRolls infiniteUnitData =
                originalUnitData.updateRolls(targetCount - totalBasicRollsFinal);
            infinitePowerStrengthAndRolls.add(infiniteUnitData);
            // update the infinite unit with the actual number of rolls that it has
            totalStrengthAndTotalRollsByUnit.put(unit, infiniteUnitData);
            activeUnits.add(unit);
          });
      activeStrengthAndRolls.addAll(infinitePowerStrengthAndRolls);
    }

    activeUnits.addAll(overstackAa);
    // Add all the overstack units
    activeStrengthAndRolls.addAll(
        overstackAa.stream()
            .map(totalStrengthAndTotalRollsByUnit::get)
            .collect(Collectors.toList()));

    // mark all of the non-active units with 0 rolls/strength
    for (final Map.Entry<Unit, UnitPowerStrengthAndRolls> mapEntry :
        totalStrengthAndTotalRollsByUnit.entrySet()) {
      if (activeUnits.contains(mapEntry.getKey())) {
        continue;
      }
      totalStrengthAndTotalRollsByUnit.put(
          mapEntry.getKey(), mapEntry.getValue().toBuilder().rolls(0).strength(0).power(0).build());
    }

    return activeStrengthAndRolls;
  }

  /**
   * @param dice Rolled Dice numbers from bridge with size equal to getTotalRolls
   * @return A list of Dice
   */
  @Override
  public List<Die> getDiceHits(final int[] dice) {
    final Deque<Integer> diceQueue =
        IntStream.of(dice).boxed().collect(Collectors.toCollection(ArrayDeque::new));

    return activeStrengthAndRolls.stream()
        .flatMap(
            unitPowerStrengthAndRolls -> {
              final int strength = unitPowerStrengthAndRolls.getStrength();
              return IntStream.range(0, unitPowerStrengthAndRolls.getRolls())
                  .mapToObj(
                      rollNumber -> {
                        final int diceValue = diceQueue.removeFirst();
                        return new Die(
                            diceValue,
                            strength,
                            diceValue < strength ? Die.DieType.HIT : Die.DieType.MISS);
                      });
            })
        .collect(Collectors.toList());
  }

  @Override
  public int calculateTotalPower() {
    return this.activeStrengthAndRolls.stream().mapToInt(UnitPowerStrengthAndRolls::getPower).sum();
  }

  @Override
  public int calculateTotalRolls() {
    return this.activeStrengthAndRolls.stream().mapToInt(UnitPowerStrengthAndRolls::getRolls).sum();
  }

  @Override
  public boolean hasStrengthOrRolls() {
    return calculateTotalRolls() != 0 && calculateTotalPower() != 0;
  }

  @Override
  public int getStrength(final Unit unit) {
    return totalStrengthAndTotalRollsByUnit.get(unit).getStrength();
  }

  @Override
  public int getRolls(final Unit unit) {
    return totalStrengthAndTotalRollsByUnit.get(unit).getRolls();
  }

  @Override
  public int getPower(final Unit unit) {
    return totalStrengthAndTotalRollsByUnit.get(unit).getPower();
  }

  public boolean isSameStrength() {
    return this.activeStrengthAndRolls.stream()
            .map(UnitPowerStrengthAndRolls::getStrength)
            .collect(Collectors.toSet())
            .size()
        == 1;
  }
}
