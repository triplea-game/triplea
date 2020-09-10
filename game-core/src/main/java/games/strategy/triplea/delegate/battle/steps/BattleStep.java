package games.strategy.triplea.delegate.battle.steps;

import games.strategy.triplea.delegate.IExecutable;
import java.util.List;

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
    SUB_OFFENSIVE_RETREAT_AFTER_BATTLE,
    SUB_DEFENSIVE_RETREAT_AFTER_BATTLE,
  }

  /** @return a list of names that will be shown in {@link games.strategy.triplea.ui.BattlePanel} */
  List<String> getNames();

  /** @return The order in which this step should be called */
  Order getOrder();
}
