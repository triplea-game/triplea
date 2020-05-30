package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import java.util.Collection;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SubsChecks {
  public static boolean defenderSubsFireFirst(
      final @NonNull Collection<Unit> attackingUnits,
      final @NonNull Collection<Unit> defendingUnits,
      final @NonNull GameData gameData) {
    return returnFireAgainstAttackingSubs(attackingUnits, defendingUnits, gameData)
            == MustFightBattle.ReturnFire.ALL
        && returnFireAgainstDefendingSubs(attackingUnits, defendingUnits, gameData)
            == MustFightBattle.ReturnFire.NONE;
  }

  public static MustFightBattle.ReturnFire returnFireAgainstAttackingSubs(
      final @NonNull Collection<Unit> attackingUnits,
      final @NonNull Collection<Unit> defendingUnits,
      final @NonNull GameData gameData) {
    final boolean attackingSubsSneakAttack =
        defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack =
        defendingSubsSneakAttackAndNoAttackingDestroyers(attackingUnits, gameData);
    final MustFightBattle.ReturnFire returnFireAgainstAttackingSubs;
    if (!attackingSubsSneakAttack) {
      returnFireAgainstAttackingSubs = MustFightBattle.ReturnFire.ALL;
    } else if (defendingSubsSneakAttack || Properties.getWW2V2(gameData)) {
      returnFireAgainstAttackingSubs = MustFightBattle.ReturnFire.SUBS;
    } else {
      returnFireAgainstAttackingSubs = MustFightBattle.ReturnFire.NONE;
    }
    return returnFireAgainstAttackingSubs;
  }

  public static MustFightBattle.ReturnFire returnFireAgainstDefendingSubs(
      final @NonNull Collection<Unit> attackingUnits,
      final @NonNull Collection<Unit> defendingUnits,
      final @NonNull GameData gameData) {
    final boolean attackingSubsSneakAttack =
        defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack =
        defendingSubsSneakAttackAndNoAttackingDestroyers(attackingUnits, gameData);
    final MustFightBattle.ReturnFire returnFireAgainstDefendingSubs;
    if (!defendingSubsSneakAttack) {
      returnFireAgainstDefendingSubs = MustFightBattle.ReturnFire.ALL;
    } else if (attackingSubsSneakAttack || Properties.getWW2V2(gameData)) {
      returnFireAgainstDefendingSubs = MustFightBattle.ReturnFire.SUBS;
    } else {
      returnFireAgainstDefendingSubs = MustFightBattle.ReturnFire.NONE;
    }
    return returnFireAgainstDefendingSubs;
  }

  private static boolean defendingSubsSneakAttackAndNoAttackingDestroyers(
      final @NonNull Collection<Unit> attackingUnits, final @NonNull GameData gameData) {
    return attackingUnits.stream().noneMatch(Matches.unitIsDestroyer())
        && defendingSubsSneakAttack(gameData);
  }

  public static boolean defendingSubsSneakAttack(final @NonNull GameData gameData) {
    return Properties.getWW2V2(gameData) || Properties.getDefendingSubsSneakAttack(gameData);
  }
}
