package games.strategy.triplea.delegate.battle.subs;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.MustFightBattle.ReturnFire;
import java.util.Collection;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Subs {

  public enum FireOrder {
    DEF_BEFORE_ATT,
    DEF_BEFORE_REGULAR,
    DEF_WITH_REGULAR
  }

  public FireOrder getFireOrder(
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final GameData gameData) {
    if (defenderSubsFireFirst(attackingUnits, defendingUnits, gameData)) {
      return FireOrder.DEF_BEFORE_ATT;
    }
    final boolean defendingSubsFireWithAllDefenders =
        !Properties.getWW2V2(gameData)
            && returnFireAgainstDefendingSubs(attackingUnits, defendingUnits, gameData)
                == ReturnFire.ALL;
    if (defendingSubsSneakAttack(gameData) && !defendingSubsFireWithAllDefenders) {
      return FireOrder.DEF_BEFORE_REGULAR;
    }
    return FireOrder.DEF_WITH_REGULAR;
  }

  public boolean defenderSubsFireFirst(
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final GameData gameData) {
    return returnFireAgainstAttackingSubs(attackingUnits, defendingUnits, gameData)
            == ReturnFire.ALL
        && returnFireAgainstDefendingSubs(attackingUnits, defendingUnits, gameData)
            == ReturnFire.NONE;
  }

  public ReturnFire returnFireAgainstAttackingSubs(
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final GameData gameData) {
    final boolean attackingSubsSneakAttack =
        defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack =
        defendingSubsSneakAttackAndNoAttackingDestroyers(attackingUnits, gameData);
    return returnFireAgainstSubs(gameData, attackingSubsSneakAttack, defendingSubsSneakAttack);
  }

  public ReturnFire returnFireAgainstDefendingSubs(
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final GameData gameData) {
    final boolean attackingSubsSneakAttack =
        defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack =
        defendingSubsSneakAttackAndNoAttackingDestroyers(attackingUnits, gameData);
    return returnFireAgainstSubs(gameData, defendingSubsSneakAttack, attackingSubsSneakAttack);
  }

  private ReturnFire returnFireAgainstSubs(
      final GameData gameData, final boolean subsSneakAttack, final boolean otherSubsSneakAttack) {
    final ReturnFire returnFireAgainstAttackingSubs;
    if (!subsSneakAttack) {
      returnFireAgainstAttackingSubs = ReturnFire.ALL;
    } else if (otherSubsSneakAttack || Properties.getWW2V2(gameData)) {
      returnFireAgainstAttackingSubs = ReturnFire.SUBS;
    } else {
      returnFireAgainstAttackingSubs = ReturnFire.NONE;
    }
    return returnFireAgainstAttackingSubs;
  }

  public boolean defendingSubsSneakAttack(final GameData gameData) {
    return Properties.getWW2V2(gameData) || Properties.getDefendingSubsSneakAttack(gameData);
  }

  private boolean defendingSubsSneakAttackAndNoAttackingDestroyers(
      final Collection<Unit> attackingUnits, final GameData gameData) {
    return attackingUnits.stream().noneMatch(Matches.unitIsDestroyer())
        && defendingSubsSneakAttack(gameData);
  }
}
