package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import java.util.Collection;

/** Logic for performing political actions. */
public interface IPoliticsDelegate extends IRemote, IDelegate {
  @RemoteActionCode(0)
  void attemptAction(PoliticalActionAttachment actionChoice);

  @RemoteActionCode(7)
  Collection<PoliticalActionAttachment> getValidActions();
}
