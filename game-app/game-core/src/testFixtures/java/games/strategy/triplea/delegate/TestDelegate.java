package games.strategy.triplea.delegate;

import games.strategy.engine.message.IRemote;
import java.io.Serializable;

/**
 * A simple dumb delegate, don't actually call these methods. Simply to satisfy the interface
 * requirements for testing.
 */
public final class TestDelegate extends AbstractDelegate {
  @Override
  public void initialize(final String name, final String displayName) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDisplayName() {
    return "displayName";
  }

  @Override
  public Class<IRemote> getRemoteType() {
    return IRemote.class;
  }

  @Override
  public Serializable saveState() {
    return null;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return true;
  }
}
