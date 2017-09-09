package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/**
 * A technology advance that allows each anti-aircraft gun to launch a free Strategic Bombing attack.
 */
public final class RocketsAdvance extends TechAdvance {
  private static final long serialVersionUID = 1526117896586201770L;

  public RocketsAdvance(final GameData data) {
    super(TECH_NAME_ROCKETS, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_ROCKETS;
  }

  @Override
  public void perform(final PlayerID id, final IDelegateBridge bridge) {}

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getRocket();
  }
}
