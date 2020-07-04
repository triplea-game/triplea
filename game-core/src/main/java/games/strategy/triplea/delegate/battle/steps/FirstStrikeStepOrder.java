package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.MustFightBattle.ReturnFire;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;

public enum FirstStrikeStepOrder {
  DEFENDER_SNEAK_ATTACK,

  DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK,
  DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE,

  DEFENDER_NO_SNEAK_ATTACK,

  OFFENDER_SNEAK_ATTACK,
  OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE,
  OFFENDER_NO_SNEAK_ATTACK;

  public static List<FirstStrikeStepOrder> calculate(final @NonNull BattleState battleState) {
    final List<FirstStrikeStepOrder> steps = new ArrayList<>();

    if (hasAttackingFirstStrike(battleState)) {
      calculateAttackerSteps(battleState).ifPresent(steps::add);
    }

    if (hasDefendingFirstStrike(battleState)) {
      calculateDefenderSteps(battleState).ifPresent(steps::add);
    }

    return steps;
  }

  private static boolean hasAttackingFirstStrike(final BattleState battleState) {
    return battleState.getAttackingUnits().stream().anyMatch(Matches.unitIsFirstStrike());
  }

  private static Optional<FirstStrikeStepOrder> calculateAttackerSteps(
      final @NonNull BattleState battleState) {
    final ReturnFire returnFireAgainstAttackingSubs =
        returnFireAgainstAttackingSubs(
            battleState.getAttackingUnits(),
            battleState.getDefendingUnits(),
            battleState.getGameData());
    switch (returnFireAgainstAttackingSubs) {
      case ALL:
        return Optional.of(OFFENDER_NO_SNEAK_ATTACK);
      case SUBS:
        return Optional.of(OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE);
      case NONE:
        return Optional.of(OFFENDER_SNEAK_ATTACK);
      default:
        return Optional.empty();
    }
  }

  private static MustFightBattle.ReturnFire returnFireAgainstAttackingSubs(
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final GameData gameData) {
    final boolean canSneakAttack = defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    final boolean enemyCanSneakAttack = defendingSubsSneakAttack(attackingUnits, gameData);
    return getReturnFire(gameData, canSneakAttack, enemyCanSneakAttack);
  }

  private static ReturnFire getReturnFire(
      final GameData gameData, final boolean canSneakAttack, final boolean enemyCanSneakAttack) {
    final ReturnFire returnFireAgainstAttackingSubs;
    if (!canSneakAttack) {
      returnFireAgainstAttackingSubs = ReturnFire.ALL;
    } else if (enemyCanSneakAttack || Properties.getWW2V2(gameData)) {
      returnFireAgainstAttackingSubs = ReturnFire.SUBS;
    } else {
      returnFireAgainstAttackingSubs = ReturnFire.NONE;
    }
    return returnFireAgainstAttackingSubs;
  }

  private static boolean hasDefendingFirstStrike(final BattleState battleState) {
    return battleState.getDefendingUnits().stream()
        .anyMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData()));
  }

  private static Optional<FirstStrikeStepOrder> calculateDefenderSteps(
      final @NonNull BattleState battleState) {
    final ReturnFire returnFireAgainstDefendingSubs =
        returnFireAgainstDefendingSubs(
            battleState.getAttackingUnits(),
            battleState.getDefendingUnits(),
            battleState.getGameData());
    switch (returnFireAgainstDefendingSubs) {
      case ALL:
        if (Properties.getWW2V2(battleState.getGameData())) {
          // ww2v2 rules require defending subs to always fire before the standard units
          return Optional.of(DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK);
        } else {
          return Optional.of(DEFENDER_NO_SNEAK_ATTACK);
        }
      case SUBS:
        return Optional.of(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE);
      case NONE:
        return Optional.of(DEFENDER_SNEAK_ATTACK);
      default:
        return Optional.empty();
    }
  }

  private static MustFightBattle.ReturnFire returnFireAgainstDefendingSubs(
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final GameData gameData) {
    final boolean canSneakAttack = defendingSubsSneakAttack(attackingUnits, gameData);
    final boolean enemyCanSneakAttack =
        defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    return getReturnFire(gameData, canSneakAttack, enemyCanSneakAttack);
  }

  private static boolean defendingSubsSneakAttack(
      final Collection<Unit> attackingUnits, final GameData gameData) {
    return attackingUnits.stream().noneMatch(Matches.unitIsDestroyer())
        && (Properties.getWW2V2(gameData) || Properties.getDefendingSubsSneakAttack(gameData));
  }
}
