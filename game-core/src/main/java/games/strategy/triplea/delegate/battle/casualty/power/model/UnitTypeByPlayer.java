package games.strategy.triplea.delegate.battle.casualty.power.model;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.UnitBattleComparator.CombatModifiers;
import lombok.Value;

@Value
public class UnitTypeByPlayer {
  UnitType unitType;
  GamePlayer gamePlayer;

  int getStrength(final CombatModifiers combatModifiers) {
    return unitType.getStrength(gamePlayer, combatModifiers);
  }

  int getDiceRolls(final CombatModifiers combatModifiers) {
    return unitType.getDiceRolls(gamePlayer, combatModifiers);
  }

  @Override
  public boolean equals(final Object rhs) {
    if (!(rhs instanceof UnitTypeByPlayer)) {
      return false;
    }

    final var other = (UnitTypeByPlayer) rhs;

    return unitType.getName().equals(other.unitType.getName())
        && gamePlayer.getName().equals(other.gamePlayer.getName());
  }

  @Override
  public int hashCode() {
    return (unitType.getName().hashCode() * 7) * (gamePlayer.getName().hashCode() * 3);
  }
}
