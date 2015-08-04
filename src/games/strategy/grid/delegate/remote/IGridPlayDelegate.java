package games.strategy.grid.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.ui.IGridPlayData;

/**
 * Implementing class is responsible for performing a turn in a Kings Table game.
 *
 */
public interface IGridPlayDelegate extends IRemote, IDelegate {
  public String play(IGridPlayData play);

  public void signalStatus(String status);
}
