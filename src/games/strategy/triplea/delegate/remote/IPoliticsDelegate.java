package games.strategy.triplea.delegate.remote;

import java.util.Collection;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;

public interface IPoliticsDelegate extends IRemote, IDelegate {
  public void attemptAction(PoliticalActionAttachment actionChoice);

  public Collection<PoliticalActionAttachment> getValidActions();
}
