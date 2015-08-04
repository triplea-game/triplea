package games.strategy.grid.checkers.delegate;

import java.io.Serializable;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.message.IRemote;


public class InitializationDelegate extends AbstractDelegate {
  /**
   * Called before the delegate will run.
   */
  @Override
  public void start() {
    super.start();
  }

  @Override
  public void end() {
    super.end();
  }

  @Override
  public Serializable saveState() {
    return null;
  }

  @Override
  public void loadState(final Serializable state) {}

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return false;
  }

  /**
   * If this class implements an interface which inherits from IRemote, returns the class of that interface.
   * Otherwise, returns null.
   */
  @Override
  public Class<? extends IRemote> getRemoteType() {
    // This class does not implement the IRemote interface, so return null.
    return null;
  }
}
