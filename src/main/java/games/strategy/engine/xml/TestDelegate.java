package games.strategy.engine.xml;

import java.io.Serializable;

import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.AbstractDelegate;

/**
 * A simple dumb delegate, dont acutally call these methods.
 * Simply to satisfy the interface requirements for testing.
 */
public final class TestDelegate extends AbstractDelegate {
  public TestDelegate() {}

  @Override
  public void initialize(final String name, final String displayName) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void end() {}

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
  public void loadState(final Serializable state) {}

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return true;
  }
}
