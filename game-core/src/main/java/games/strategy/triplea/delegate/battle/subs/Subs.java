package games.strategy.triplea.delegate.battle.subs;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.MustFightBattle.ReturnFire;
import java.util.Collection;
import lombok.experimental.UtilityClass;

/** Utility class to work with subs and their firing order. */
@UtilityClass
public class Subs {

  public enum FireOrder {
    DEF_BEFORE_ATT,
    DEF_BEFORE_REGULAR,
    DEF_WITH_REGULAR
  }

  public static FireOrder getFireOrder(
      final GameData gameData,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits) {
    if (defenderSubsFireFirst(gameData, attackingUnits, defendingUnits)) {
      return FireOrder.DEF_BEFORE_ATT;
    }
    final boolean defendingSubsFireWithAllDefenders =
        !Properties.getWW2V2(gameData)
            && returnFireAgainstDefendingSubs(gameData, attackingUnits, defendingUnits)
                == ReturnFire.ALL;
    if (defendingSubsSneakAttack(gameData) && !defendingSubsFireWithAllDefenders) {
      return FireOrder.DEF_BEFORE_REGULAR;
    }
    return FireOrder.DEF_WITH_REGULAR;
  }

  public static boolean defenderSubsFireFirst(
      final GameData gameData,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits) {
    return returnFireAgainstAttackingSubs(gameData, attackingUnits, defendingUnits)
            == ReturnFire.ALL
        && returnFireAgainstDefendingSubs(gameData, attackingUnits, defendingUnits)
            == ReturnFire.NONE;
  }

  public static ReturnFire returnFireAgainstAttackingSubs(
      final GameData gameData,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits) {
    final boolean attackingSubsSneakAttack =
        defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack =
        defendingSubsSneakAttackAndNoAttackingDestroyers(gameData, attackingUnits);
    return returnFireAgainstSubs(gameData, attackingSubsSneakAttack, defendingSubsSneakAttack);
  }

  public static ReturnFire returnFireAgainstDefendingSubs(
      final GameData gameData,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits) {
    final boolean attackingSubsSneakAttack =
        defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack =
        defendingSubsSneakAttackAndNoAttackingDestroyers(gameData, attackingUnits);
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

  public static boolean defendingSubsSneakAttack(final GameData gameData) {
    return Properties.getWW2V2(gameData) || Properties.getDefendingSubsSneakAttack(gameData);
  }

  private boolean defendingSubsSneakAttackAndNoAttackingDestroyers(
      final GameData gameData, final Collection<Unit> attackingUnits) {
    return attackingUnits.stream().noneMatch(Matches.unitIsDestroyer())
        && defendingSubsSneakAttack(gameData);
  }
}
