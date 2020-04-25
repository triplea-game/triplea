package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import java.io.Serializable;

/**
 * Logic for posting a save game to a forum. Supplements other game logic at points where it makes
 * sense to record a save game (e.g. at the end of a game turn).
 */
public interface IAbstractForumPosterDelegate extends IRemote, IDelegate {
  @RemoteActionCode(9)
  boolean postTurnSummary(PbemMessagePoster poster, String title);

  @RemoteActionCode(12)
  void setHasPostedTurnSummary(boolean hasPostedTurnSummary);

  @RemoteActionCode(4)
  boolean getHasPostedTurnSummary();

  @RemoteActionCode(7)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(11)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(13)
  @Override
  void start();

  @RemoteActionCode(1)
  @Override
  void end();

  @RemoteActionCode(5)
  @Override
  String getName();

  @RemoteActionCode(3)
  @Override
  String getDisplayName();

  @RemoteActionCode(2)
  @Override
  IDelegateBridge getBridge();

  @RemoteActionCode(10)
  @Override
  Serializable saveState();

  @RemoteActionCode(8)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(6)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(0)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
