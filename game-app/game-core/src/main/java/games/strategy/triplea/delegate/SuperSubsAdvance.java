package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/** A technology advance that improves the attack of all submarines. */
public final class SuperSubsAdvance extends TechAdvance {
  private static final long serialVersionUID = -5469354766630425933L;

  public SuperSubsAdvance(final GameData data) {
    super(TECH_NAME_SUPER_SUBS, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_SUPER_SUBS;
  }

  @Override
  public void perform(final GamePlayer gamePlayer, final IDelegateBridge bridge) {}

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getSuperSub();
  }
}
