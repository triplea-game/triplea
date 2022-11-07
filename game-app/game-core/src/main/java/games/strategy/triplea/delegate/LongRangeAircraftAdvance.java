package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/** A technology advance that increases the range of all aircraft by two. */
public final class LongRangeAircraftAdvance extends TechAdvance {
  private static final long serialVersionUID = 1986380888336238652L;

  public LongRangeAircraftAdvance(final GameData data) {
    super(TECH_NAME_LONG_RANGE_AIRCRAFT, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_LONG_RANGE_AIRCRAFT;
  }

  @Override
  public void perform(final GamePlayer gamePlayer, final IDelegateBridge bridge) {}

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getLongRangeAir();
  }
}
