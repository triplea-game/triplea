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

  public boolean supportsTransactions() {
    return false;
  }

  public void initialize(final String name) {
    m_name = name;
  }

  @Override
  public void initialize(final String name, final String displayName) {
    m_name = name;
  }

  public void startTransaction() {}

  public void rollback() {}

  public void commit() {}

  public boolean inTransaction() {
    return false;
  }

  @Override
  public String getName() {
    return m_name;
  }

  public void cancelTransaction() {}

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

  /**
   * Returns the state of the Delegate.
   */
  @Override
  public Serializable saveState() {
    return null;
  }

  /**
   * Loads the delegates state
   */
  @Override
  public void loadState(final Serializable state) {}

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return true;
  }
}
