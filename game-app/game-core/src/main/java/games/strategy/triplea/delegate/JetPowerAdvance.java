package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/** A technology advance that allows the creation of jet fighters. */
public final class JetPowerAdvance extends TechAdvance {
  private static final long serialVersionUID = -9124162661008361132L;

  public JetPowerAdvance(final GameData data) {
    super(TECH_NAME_JET_POWER, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_JET_POWER;
  }

  @Override
  public void perform(final GamePlayer gamePlayer, final IDelegateBridge bridge) {}

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getJetPower();
  }
}
