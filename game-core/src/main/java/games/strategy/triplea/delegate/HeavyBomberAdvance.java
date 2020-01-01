package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/** A technology advance that improves bombers to heavy bombers. */
public final class HeavyBomberAdvance extends TechAdvance {
  private static final long serialVersionUID = -1743063539572684675L;

  public HeavyBomberAdvance(final GameData data) {
    super(TECH_NAME_HEAVY_BOMBER, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_HEAVY_BOMBER;
  }

  @Override
  public void perform(final GamePlayer gamePlayer, final IDelegateBridge bridge) {}

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getHeavyBomber();
  }
}
