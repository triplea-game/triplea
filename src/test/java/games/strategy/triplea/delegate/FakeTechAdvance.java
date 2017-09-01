package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/**
 * Fake implementation of {@link TechAdvance} useful for testing.
 */
public final class FakeTechAdvance extends TechAdvance {
  private static final long serialVersionUID = -7878431004713814054L;

  public FakeTechAdvance(final GameData data, final String name) {
    super(name, data);
  }

  @Override
  public String getProperty() {
    return getName().replace(' ', '_');
  }

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return false;
  }

  @Override
  public void perform(final PlayerID id, final IDelegateBridge bridge) {}
}
