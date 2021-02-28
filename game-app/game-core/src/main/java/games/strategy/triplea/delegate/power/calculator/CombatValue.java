package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.Comparator;

public interface CombatValue {

  RollCalculator getRoll();

  StrengthCalculator getStrength();

  default PowerCalculator getPower() {
    return new PowerCalculator(getStrength(), getRoll(), this::chooseBestRoll, this::getDiceSides);
  }

  /**
   * Sorts units from high strength to low strength
   *
   * <p>Takes into account different dice sides. A unit with strength 2 and dice sides 2 will be
   * sorted high than a unit with strength 3 and dice sides of 6.
   */
  default Comparator<Unit> unitComparator() {
    // unit support is stateful which would mess up the sort calculations so remove unit supports
    final StrengthCalculator strengthCalculator = this.buildWithNoUnitSupports().getStrength();
    return Comparator.<Unit, Boolean>comparing(
            unit -> strengthCalculator.getStrength(unit).getValue() == 0)
        .thenComparingDouble(
            unit ->
                -strengthCalculator.getStrength(unit).getValue() / (float) this.getDiceSides(unit));
  }

  int getDiceSides(Unit unit);

  BattleState.Side getBattleSide();

  boolean chooseBestRoll(Unit unit);

  Collection<Unit> getFriendUnits();

  Collection<Unit> getEnemyUnits();

  CombatValue buildWithNoUnitSupports();

  CombatValue buildOppositeCombatValue();
}
