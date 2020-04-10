package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Territory;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import games.strategy.triplea.delegate.UndoableMove;
import java.util.Collection;
import java.util.List;

/** Remote interface for MoveDelegate. */
public interface IMoveDelegate
    extends IAbstractMoveDelegate<UndoableMove>, IAbstractForumPosterDelegate {
  /**
   * Performs the specified move.
   *
   * @param move - the move to perform.
   * @return an error message if the move can't be made, null otherwise
   */
  @RemoteActionCode(13)
  String performMove(MoveDescription move);

  /**
   * Get what air units must move before the end of the players turn.
   *
   * @param player referring player ID
   * @return a list of territories with air units that must move of player ID
   */
  @RemoteActionCode(9)
  Collection<Territory> getTerritoriesWhereAirCantLand(GamePlayer player);

  @RemoteActionCode(8)
  Collection<Territory> getTerritoriesWhereAirCantLand();

  /**
   * Get what units must have combat ability.
   *
   * @return a list of Territories with units that can't fight
   */
  @RemoteActionCode(10)
  Collection<Territory> getTerritoriesWhereUnitsCantFight();

  @RemoteActionCode(17)
  @Override
  void setHasPostedTurnSummary(boolean hasPostedTurnSummary);

  @RemoteActionCode(14)
  @Override
  boolean postTurnSummary(PbemMessagePoster poster, String title);

  @RemoteActionCode(19)
  @Override
  String undoMove(int moveIndex);

  @RemoteActionCode(5)
  @Override
  List<UndoableMove> getMovesMade();
}
