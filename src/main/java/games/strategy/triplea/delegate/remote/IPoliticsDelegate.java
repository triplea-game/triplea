package games.strategy.triplea.delegate.remote;

import java.util.Collection;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.attachments.PoliticalActionAttachment;

public interface IPoliticsDelegate extends IRemote, IDelegate {
  void attemptAction(PoliticalActionAttachment actionChoice);

  Collection<PoliticalActionAttachment> getValidActions();
}
