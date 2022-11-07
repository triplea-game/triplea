package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/** A technology advance that allows artillery to support multiple infantry. */
public final class ImprovedArtillerySupportAdvance extends TechAdvance {
  private static final long serialVersionUID = 3946378995070209879L;

  public ImprovedArtillerySupportAdvance(final GameData data) {
    super(TECH_NAME_IMPROVED_ARTILLERY_SUPPORT, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_IMPROVED_ARTILLERY_SUPPORT;
  }

  @Override
  public void perform(final GamePlayer gamePlayer, final IDelegateBridge bridge) {}

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getImprovedArtillerySupport();
  }
}
