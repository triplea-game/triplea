package games.strategy.triplea.delegate.battle.casualty;

import com.google.common.collect.Lists;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.power.calculator.AaPowerStrengthAndRolls;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;

@Value
class LowLuckTargetGroups {
  List<List<Unit>> guaranteedHitGroups;
  List<Unit> remainderUnits;

  /**
   * Categorize the units and then split them up into groups of guaranteeHitGroupSize. See {@link
   * LowLuckTargetGroups#calculateGuaranteeLowLuckHitGroupSize(Collection, DiceRoll,
   * AaPowerStrengthAndRolls)}
   *
   * <p>Any group less than guaranteeHitGroupSize is added to the remainderUnits
   */
  LowLuckTargetGroups(
      final Collection<Unit> targets,
      final DiceRoll diceRoll,
      final AaPowerStrengthAndRolls unitPowerAndRollsMap) {

    final int guaranteeHitGroupSize =
        calculateGuaranteeLowLuckHitGroupSize(targets, diceRoll, unitPowerAndRollsMap);

    this.guaranteedHitGroups = new ArrayList<>();
    this.remainderUnits = new ArrayList<>();
    if (guaranteeHitGroupSize == 0) {
      // it isn't possible to create guaranteed hit groups so all the units are put in the remainder
      this.remainderUnits.addAll(targets);
      return;
    }

    final Collection<UnitCategory> groupedTargets =
        UnitSeparator.categorize(targets, null, false, true);
    for (final UnitCategory uc : groupedTargets) {
      final Deque<List<Unit>> guaranteedGroups =
          new ArrayDeque<>(Lists.partition(uc.getUnits(), guaranteeHitGroupSize));
      if (guaranteedGroups.isEmpty()) {
        continue;
      }
      final List<Unit> lastGroup = guaranteedGroups.peekLast();
      // if the last group isn't the right size, put those units in the remainder list
      if (lastGroup.size() != guaranteeHitGroupSize) {
        final List<Unit> groupOfRemainderUnits = guaranteedGroups.removeLast();
        this.remainderUnits.addAll(groupOfRemainderUnits);
      }
      this.guaranteedHitGroups.addAll(guaranteedGroups);
    }
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

    // if the aa units aren't the same strength, then it isn't possible to calculate the target
    // count for a guaranteed hit because different strengths have different target counts.
    if (!unitPowerAndRollsMap.isSameStrength()
        // if there are more hits than (strength * targetCount / diceSides), then there must have
        // been AA that can fire multiple times at a single target (overstackAA) and that messes up
        // the target count.
        || (diceRoll.getHits()
            > Math.ceil(
                (double) (bestStrength * availableTargets.size()) / (double) chosenDiceSize))
        // if the best strength isn't a factor of chosenDiceSize, then the target count will be
        // fractional which doesn't work
        || chosenDiceSize % bestStrength != 0) {
      return 0;
    }

    return chosenDiceSize / bestStrength;
  }

  boolean hasGuaranteedGroups() {
    return !guaranteedHitGroups.isEmpty();
  }

  /** Grab one unit from each of the guaranteed hit groups */
  List<Unit> getGuaranteedHits() {
    return guaranteedHitGroups.stream()
        .map(hitGroup -> hitGroup.get(0))
        .collect(Collectors.toList());
  }
}
