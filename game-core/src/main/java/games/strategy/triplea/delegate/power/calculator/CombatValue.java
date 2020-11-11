package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;

public interface CombatValue {

  RollCalculator getRoll();

  StrengthCalculator getStrength();

  default PowerCalculator getPower() {
    return new PowerCalculator(getStrength(), getRoll(), this::chooseBestRoll, this::getDiceSides);
  }

  int getDiceSides(Unit unit);

  BattleState.Side getBattleSide();

  boolean chooseBestRoll(Unit unit);

  Collection<Unit> getFriendUnits();

  Collection<Unit> getEnemyUnits();

  CombatValue buildWithNoUnitSupports();

  CombatValue buildOppositeCombatValue();
}
