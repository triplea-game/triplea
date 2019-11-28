package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.UndoableMove;
import java.util.Collection;

/** Remote interface for MoveDelegate. */
public interface IMoveDelegate
    extends IAbstractMoveDelegate<UndoableMove>, IAbstractForumPosterDelegate {
  /**
   * Performs the specified move.
   *
   * @param move - the move to perform.
   * @return an error message if the move can't be made, null otherwise
   */
  String performMove(MoveDescription move);

  /**
   * Get what air units must move before the end of the players turn.
   *
   * @param player referring player ID
   * @return a list of territories with air units that must move of player ID
   */
  Collection<Territory> getTerritoriesWhereAirCantLand(PlayerId player);

  Collection<Territory> getTerritoriesWhereAirCantLand();

  /**
   * Get what units must have combat ability.
   *
   * @return a list of Territories with units that can't fight
   */
  Collection<Territory> getTerritoriesWhereUnitsCantFight();
}
