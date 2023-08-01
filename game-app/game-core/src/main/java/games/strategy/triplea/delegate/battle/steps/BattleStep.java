package games.strategy.triplea.delegate.battle.steps;

import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.change.CheckGeneralBattleEnd;
import games.strategy.triplea.delegate.battle.steps.change.CheckStalemateBattleEnd;
import games.strategy.triplea.delegate.battle.steps.change.ClearAaCasualties;
import games.strategy.triplea.delegate.battle.steps.change.ClearBombardmentCasualties;
import games.strategy.triplea.delegate.battle.steps.change.ClearGeneralCasualties;
import games.strategy.triplea.delegate.battle.steps.change.LandParatroopers;
import games.strategy.triplea.delegate.battle.steps.change.MarkNoMovementLeft;
import games.strategy.triplea.delegate.battle.steps.change.RemoveNonCombatants;
import games.strategy.triplea.delegate.battle.steps.change.RemoveUnprotectedUnits;
import games.strategy.triplea.delegate.battle.steps.change.RemoveUnprotectedUnitsGeneral;
import games.strategy.triplea.delegate.battle.steps.change.suicide.RemoveFirstStrikeSuicide;
import games.strategy.triplea.delegate.battle.steps.change.suicide.RemoveGeneralSuicide;
import games.strategy.triplea.delegate.battle.steps.fire.NavalBombardment;
import games.strategy.triplea.delegate.battle.steps.fire.aa.DefensiveAaFire;
import games.strategy.triplea.delegate.battle.steps.fire.aa.OffensiveAaFire;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.ClearFirstStrikeCasualties;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.DefensiveFirstStrike;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.OffensiveFirstStrike;
import games.strategy.triplea.delegate.battle.steps.fire.general.DefensiveGeneral;
import games.strategy.triplea.delegate.battle.steps.fire.general.OffensiveGeneral;
import games.strategy.triplea.delegate.battle.steps.retreat.DefensiveSubsRetreat;
import games.strategy.triplea.delegate.battle.steps.retreat.OffensiveGeneralRetreat;
import games.strategy.triplea.delegate.battle.steps.retreat.OffensiveSubsRetreat;
import games.strategy.triplea.delegate.battle.steps.retreat.sub.SubmergeSubsVsOnlyAirStep;
import java.util.List;
import lombok.Value;

/**
 * This is used to break up the battle into separate atomic pieces. If there is a network error, or
 * some other unfortunate event, then we need to keep track of what pieces we have executed, and
 * what is left to do. Each atomic step is in its own BattleAtomic with the definition of atomic is
 * that either:
 *
 * <ol>
 *   <li>The code does not use IDelegateBridge
 *   <li>If the code uses IDelegateBridge, and an exception is called from one of those methods, the
 *       exception will be propagated out of execute() and the execute method can be called again.
 * </ol>
 */
public interface BattleStep extends IExecutable {

  enum Order {
    AA_OFFENSIVE,
    AA_DEFENSIVE,
    AA_REMOVE_CASUALTIES,
    NAVAL_BOMBARDMENT,
    NAVAL_BOMBARDMENT_REMOVE_CASUALTIES,
    REMOVE_NON_COMBATANTS,
    LAND_PARATROOPERS,
    MARK_NO_MOVEMENT_LEFT,
    SUB_OFFENSIVE_RETREAT_BEFORE_BATTLE,
    SUB_DEFENSIVE_RETREAT_BEFORE_BATTLE,
    REMOVE_UNPROTECTED_UNITS,
    SUBMERGE_SUBS_VS_ONLY_AIR,
    FIRST_STRIKE_OFFENSIVE,
    FIRST_STRIKE_DEFENSIVE,
    FIRST_STRIKE_REMOVE_CASUALTIES,
    FIRST_STRIKE_SUICIDE_REMOVE_CASUALTIES,
    FIRST_STRIKE_OFFENSIVE_REGULAR,
    AIR_OFFENSIVE_NON_SUBS,
    GENERAL_OFFENSIVE,
    FIRST_STRIKE_DEFENSIVE_REGULAR,
    AIR_DEFENSIVE_NON_SUBS,
    GENERAL_DEFENSIVE,
    GENERAL_REMOVE_CASUALTIES,
    SUICIDE_REMOVE_CASUALTIES,
    REMOVE_UNPROTECTED_UNITS_GENERAL,
    GENERAL_BATTLE_END_CHECK,
    SUB_OFFENSIVE_RETREAT_AFTER_BATTLE,
    OFFENSIVE_GENERAL_RETREAT,
    STALEMATE_BATTLE_END_CHECK,
    SUB_DEFENSIVE_RETREAT_AFTER_BATTLE,

    FIRE_ROUND_ROLL_DICE,
    FIRE_ROUND_SELECT_CASUALTIES,
    FIRE_ROUND_REMOVE_CASUALTIES,
  }

  @Value
  class StepDetails {
    String name;
    BattleStep step;
  }

  /**
   * @return a list of names that will be shown in the UI.
   */
  List<StepDetails> getAllStepDetails();

  /**
   * @return The order in which this step should be called
   */
  Order getOrder();

  static List<BattleStep> getAll(final BattleState battleState, final BattleActions battleActions) {
    return List.of(
        new OffensiveAaFire(battleState, battleActions),
        new DefensiveAaFire(battleState, battleActions),
        new SubmergeSubsVsOnlyAirStep(battleState, battleActions),
        new RemoveUnprotectedUnits(battleState, battleActions),
        new NavalBombardment(battleState, battleActions),
        new ClearBombardmentCasualties(battleState, battleActions),
        new LandParatroopers(battleState, battleActions),
        new OffensiveSubsRetreat(battleState, battleActions),
        new DefensiveSubsRetreat(battleState, battleActions),
        new OffensiveFirstStrike(battleState, battleActions),
        new DefensiveFirstStrike(battleState, battleActions),
        new ClearFirstStrikeCasualties(battleState, battleActions),
        new OffensiveGeneral(battleState, battleActions),
        new DefensiveGeneral(battleState, battleActions),
        new ClearAaCasualties(battleState, battleActions),
        new RemoveNonCombatants(battleState, battleActions),
        new MarkNoMovementLeft(battleState, battleActions),
        new RemoveFirstStrikeSuicide(battleState, battleActions),
        new RemoveGeneralSuicide(battleState, battleActions),
        new OffensiveGeneralRetreat(battleState, battleActions),
        new ClearGeneralCasualties(battleState, battleActions),
        new RemoveUnprotectedUnitsGeneral(battleState, battleActions),
        new CheckGeneralBattleEnd(battleState, battleActions),
        new CheckStalemateBattleEnd(battleState, battleActions));
  }
}
