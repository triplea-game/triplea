package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.triplea.attachments.PoliticalActionAttachment;

import java.io.Serializable;
import java.util.Collection;

/** Logic for performing political actions. */
public interface IPoliticsDelegate extends IRemote, IDelegate {
  @RemoteActionCode(0)
  void attemptAction(PoliticalActionAttachment actionChoice);

  @RemoteActionCode(7)
  Collection<PoliticalActionAttachment> getValidActions();

  @RemoteActionCode(8)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(11)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(12)
  @Override
  void start();

  @RemoteActionCode(2)
  @Override
  void end();

  @RemoteActionCode(5)
  @Override
  String getName();

  @RemoteActionCode(4)
  @Override
  String getDisplayName();

  @RemoteActionCode(3)
  @Override
  IDelegateBridge getBridge();

  @RemoteActionCode(10)
  @Override
  Serializable saveState();

  @RemoteActionCode(9)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(6)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(1)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
