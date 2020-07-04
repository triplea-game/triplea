package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.MustFightBattle.ReturnFire;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

@AllArgsConstructor
public enum FirstStrikeStepOrder {
  DEFENDER_SNEAK_ATTACK(ReturnFire.NONE),

  DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK(ReturnFire.ALL),
  DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE(ReturnFire.SUBS),

  DEFENDER_NO_SNEAK_ATTACK(ReturnFire.ALL),

  OFFENDER_SNEAK_ATTACK(ReturnFire.NONE),
  OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE(ReturnFire.SUBS),
  OFFENDER_NO_SNEAK_ATTACK(ReturnFire.ALL),

  NOT_APPLICABLE(ReturnFire.ALL);

  @Getter private final ReturnFire returnFire;

  public static FirstStrikeResult calculate(final @NonNull BattleState battleState) {
    final FirstStrikeResult.FirstStrikeResultBuilder result = FirstStrikeResult.builder();

    if (hasAttackingFirstStrike(battleState)) {
      result.attacker(calculateAttackerSteps(battleState));
    }

    if (hasDefendingFirstStrike(battleState)) {
      result.defender(calculateDefenderSteps(battleState));
    }

    return result.build();
  }

  @Value
  @Builder
  public static class FirstStrikeResult {
    @Builder.Default FirstStrikeStepOrder attacker = NOT_APPLICABLE;
    @Builder.Default FirstStrikeStepOrder defender = NOT_APPLICABLE;
  }

  private static boolean hasAttackingFirstStrike(final BattleState battleState) {
    return battleState.getAttackingUnits().stream().anyMatch(Matches.unitIsFirstStrike());
  }

  private static FirstStrikeStepOrder calculateAttackerSteps(
      final @NonNull BattleState battleState) {
    final ReturnFire returnFireAgainstAttackingSubs =
        returnFireAgainstAttackingSubs(
            battleState.getAttackingUnits(),
            battleState.getDefendingUnits(),
            battleState.getGameData());
    switch (returnFireAgainstAttackingSubs) {
      case ALL:
        return OFFENDER_NO_SNEAK_ATTACK;
      case SUBS:
        return OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE;
      case NONE:
        return OFFENDER_SNEAK_ATTACK;
      default:
        return NOT_APPLICABLE;
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

  private static FirstStrikeStepOrder calculateDefenderSteps(
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
          return DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK;
        } else {
          return DEFENDER_NO_SNEAK_ATTACK;
        }
      case SUBS:
        return DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE;
      case NONE:
        return DEFENDER_SNEAK_ATTACK;
      default:
        return NOT_APPLICABLE;
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
