package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import games.strategy.triplea.delegate.UndoableMove;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

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
  @Nullable String undoMove(int moveIndex);

  @RemoteActionCode(5)
  @Override
  List<UndoableMove> getMovesMade();

  @RemoteActionCode(11)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(16)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(18)
  @Override
  void start();

  @RemoteActionCode(1)
  @Override
  void end();

  @RemoteActionCode(6)
  @Override
  String getName();

  @RemoteActionCode(3)
  @Override
  String getDisplayName();

  @RemoteActionCode(2)
  @Override
  IDelegateBridge getBridge();

  @RemoteActionCode(15)
  @Override
  Serializable saveState();

  @RemoteActionCode(12)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(7)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(0)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
