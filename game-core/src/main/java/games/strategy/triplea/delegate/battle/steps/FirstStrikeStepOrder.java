package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.MustFightBattle.ReturnFire;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum FirstStrikeStepOrder {
  FIRST_STRIKE_DEFENDER_FIRST_NONE,

  FIRST_STRIKE_DEFENDER_SECOND_ALL,
  FIRST_STRIKE_DEFENDER_SECOND_SUBS,
  
  FIRST_STRIKE_DEFENDER_STANDARD_ALL,

  FIRST_STRIKE_OFFENDER_ALL,
  FIRST_STRIKE_OFFENDER_SUBS,
  FIRST_STRIKE_OFFENDER_NONE;



  public static List<FirstStrikeStepOrder> calculate(final BattleState battleState) {
    final List<FirstStrikeStepOrder> steps = new ArrayList<>();

    if (!(battleState.getAttackingUnits().stream().anyMatch(Matches.unitIsFirstStrike())
        || battleState.getDefendingUnits().stream().anyMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData())))) {
      return steps;
    }

    final boolean defenderSubsFireFirst =
        SubsChecks.defenderSubsFireFirst(battleState.getAttackingUnits(), battleState.getDefendingUnits(), battleState.getGameData());
    final ReturnFire returnFireAgainstAttackingSubs =
        SubsChecks.returnFireAgainstAttackingSubs(battleState.getAttackingUnits(), battleState.getDefendingUnits(), battleState.getGameData());
    final ReturnFire returnFireAgainstDefendingSubs =
        SubsChecks.returnFireAgainstDefendingSubs(battleState.getAttackingUnits(), battleState.getDefendingUnits(), battleState.getGameData());
    // if attacker has no sneak attack subs, then defender sneak attack subs fire first and remove
    // casualties
    if (defenderSubsFireFirst
        && battleState.getDefendingUnits().stream().anyMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData()))) {
      steps.add(FIRST_STRIKE_DEFENDER_FIRST_NONE);
    }
    final boolean onlyAttackerSneakAttack =
        !defenderSubsFireFirst
            && returnFireAgainstAttackingSubs == ReturnFire.NONE
            && returnFireAgainstDefendingSubs == ReturnFire.ALL;

    if (battleState.getAttackingUnits().stream().anyMatch(Matches.unitIsFirstStrike())) {
      if (onlyAttackerSneakAttack) {
        steps.add(FIRST_STRIKE_OFFENDER_NONE);
      } else {
        switch (returnFireAgainstAttackingSubs) {
          case ALL:
            steps.add(FIRST_STRIKE_OFFENDER_ALL);
            break;
          case SUBS:
            steps.add(FIRST_STRIKE_OFFENDER_SUBS);
            break;
          default:
            throw new IllegalStateException("Invalid return fire");
        }
      }
    }
    // ww2v2 rules, all subs fire FIRST in combat, regardless of presence of destroyers.
    final boolean defendingSubsFireWithAllDefenders =
        !defenderSubsFireFirst
            && !Properties.getWW2V2(battleState.getGameData())
            && returnFireAgainstDefendingSubs == ReturnFire.ALL;
    // defender subs sneak attack, no sneak attack in Pacific/Europe Theaters or if destroyers are
    // present
    final boolean defendingSubsFireWithAllDefendersAlways =
        !SubsChecks.defendingSubsSneakAttack(battleState.getGameData());
    if (!defendingSubsFireWithAllDefendersAlways
        && !defendingSubsFireWithAllDefenders
        && !defenderSubsFireFirst
        && battleState.getDefendingUnits().stream().anyMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData()))) {
      switch (returnFireAgainstDefendingSubs) {
        case ALL:
          steps.add(FIRST_STRIKE_DEFENDER_SECOND_ALL);
          break;
        case SUBS:
          steps.add(FIRST_STRIKE_DEFENDER_SECOND_SUBS);
          break;
        default:
          throw new IllegalStateException("Invalid return fire");
      }
    }
    if ((battleState.getAttackingUnits().stream().anyMatch(Matches.unitIsFirstStrike())
        || battleState.getDefendingUnits().stream().anyMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData())))
        && !defenderSubsFireFirst
        && !onlyAttackerSneakAttack
        && ((returnFireAgainstDefendingSubs != ReturnFire.ALL
        && battleState.getDefendingUnits().stream().anyMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData())))
        || (returnFireAgainstAttackingSubs != ReturnFire.ALL
        && battleState.getAttackingUnits().stream().anyMatch(Matches.unitIsFirstStrike())))) {

      if (steps.contains(FIRST_STRIKE_OFFENDER_SUBS)
          || steps.contains(FIRST_STRIKE_DEFENDER_SECOND_SUBS)) {
        //steps.add(FIRST_STRIKE_REMOVE_CASUALTIES_FROM_FIRST_STRIKE);
      }
    }

    // classic rules, subs fire with all defenders
    // also, ww2v3/global rules, defending subs without sneak attack fire with all defenders
    final Collection<Unit> defendingUnitsAliveAndDamaged = new ArrayList<>(battleState.getDefendingUnits());
    defendingUnitsAliveAndDamaged.addAll(battleState.getDefendingWaitingToDie());
    if (defendingUnitsAliveAndDamaged.stream()
        .anyMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData()))
        && !defenderSubsFireFirst
        && (defendingSubsFireWithAllDefenders || defendingSubsFireWithAllDefendersAlways)) {
      steps.add(FIRST_STRIKE_DEFENDER_STANDARD_ALL);
    }

    if ((returnFireAgainstAttackingSubs == ReturnFire.ALL
        && battleState.getAttackingUnits().stream().anyMatch(Matches.unitIsFirstStrike()))
        || (returnFireAgainstDefendingSubs == ReturnFire.ALL
        && battleState.getDefendingUnits().stream().anyMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData())))) {
      if (steps.contains(FIRST_STRIKE_OFFENDER_ALL)
          || steps.contains(FIRST_STRIKE_DEFENDER_SECOND_ALL)
          || steps.contains(FIRST_STRIKE_DEFENDER_STANDARD_ALL)) {
        //steps.add(FIRST_STRIKE_REMOVE_CASUALTIES);
      }
    }

    return steps;
  }

  public static List<FirstStrikeStepOrder> calculate2(final BattleState battleState) {
    final List<FirstStrikeStepOrder> steps = new ArrayList<>();

    if (!(battleState.getAttackingUnits().stream().anyMatch(Matches.unitIsFirstStrike())
        || battleState.getDefendingUnits().stream().anyMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData())))) {
      return steps;
    }

    final ReturnFire returnFireAgainstDefendingSubs =
        SubsChecks.returnFireAgainstDefendingSubs(battleState.getAttackingUnits(), battleState.getDefendingUnits(), battleState.getGameData());
    if (SubsChecks.defenderSubsFireFirst(battleState.getAttackingUnits(), battleState.getDefendingUnits(), battleState.getGameData())) {
      if (battleState.getDefendingUnits().stream()
          .anyMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData()))) {
        switch (returnFireAgainstDefendingSubs) {
          case NONE:
            steps.add(FIRST_STRIKE_DEFENDER_FIRST_NONE);
            break;
          default:
            throw new IllegalStateException("Invalid return fire");
        }
      }
    }
    final MustFightBattle.ReturnFire returnFireAgainstAttackingSubs =
        SubsChecks.returnFireAgainstAttackingSubs(
            battleState.getAttackingUnits(),
            battleState.getDefendingUnits(),
            battleState.getGameData());
    if (battleState.getAttackingUnits().stream().anyMatch(Matches.unitIsFirstStrike())) {
      switch (returnFireAgainstAttackingSubs) {
        case ALL:
          steps.add(FIRST_STRIKE_OFFENDER_ALL);
          break;
        case SUBS:
          steps.add(FIRST_STRIKE_OFFENDER_SUBS);
          break;
        case NONE:
          steps.add(FIRST_STRIKE_OFFENDER_NONE);
          break;
        default:
          throw new IllegalStateException("Invalid return fire");
      }
    }

    final boolean defendingSubsFireWithAllDefenders =
        !SubsChecks.defenderSubsFireFirst(battleState.getAttackingUnits(), battleState.getDefendingUnits(), battleState.getGameData())
            && !Properties.getWW2V2(battleState.getGameData())
            && SubsChecks.returnFireAgainstDefendingSubs(battleState.getAttackingUnits(), battleState.getDefendingUnits(), battleState.getGameData())
            == ReturnFire.ALL;
    if (SubsChecks.defendingSubsSneakAttack(battleState.getGameData())
        && !SubsChecks.defenderSubsFireFirst(battleState.getAttackingUnits(), battleState.getDefendingUnits(), battleState.getGameData())
        && !defendingSubsFireWithAllDefenders) {
      if (battleState.getDefendingUnits().stream()
          .anyMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData()))) {
        switch (returnFireAgainstDefendingSubs) {
          case ALL:
            steps.add(FIRST_STRIKE_DEFENDER_SECOND_ALL);
            break;
          case SUBS:
            steps.add(FIRST_STRIKE_DEFENDER_SECOND_SUBS);
            break;
          default:
            throw new IllegalStateException("Invalid return fire");
        }
      }
   }

    if (!SubsChecks.defenderSubsFireFirst(battleState.getAttackingUnits(), battleState.getDefendingUnits(), battleState.getGameData())
        && (!SubsChecks.defendingSubsSneakAttack(battleState.getGameData()) || defendingSubsFireWithAllDefenders)) {
      if (battleState.getDefendingUnits().stream()
          .anyMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData()))) {
        switch (returnFireAgainstDefendingSubs) {
          case ALL:
            steps.add(FIRST_STRIKE_DEFENDER_STANDARD_ALL);
            break;
          default:
            throw new IllegalStateException("Invalid return fire");
        }
      }
    }

    return steps;
  }
}
