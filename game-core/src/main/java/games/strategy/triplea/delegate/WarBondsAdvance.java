package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/** A technology advance that increases income by d6 per turn. */
public final class WarBondsAdvance extends TechAdvance {
  private static final long serialVersionUID = -9048146216351059811L;

  public WarBondsAdvance(final GameData data) {
    super(TECH_NAME_WAR_BONDS, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_WAR_BONDS;
  }

  @Override
  public void perform(final GamePlayer gamePlayer, final IDelegateBridge bridge) {}

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getWarBonds();
  }
}
