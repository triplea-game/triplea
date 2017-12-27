package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/**
 * A technology advance that provides anti-aircraft radar.
 */
final class AARadarAdvance extends TechAdvance {
  private static final long serialVersionUID = 6464021231625252901L;

  public AARadarAdvance(final GameData data) {
    super(TECH_NAME_AA_RADAR, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_AA_RADAR;
  }

  @Override
  public void perform(final PlayerID id, final IDelegateBridge bridge) {}

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getAARadar();
  }
}
