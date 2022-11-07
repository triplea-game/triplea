package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/** A technology advance that allows the use of mechanized infantry units. */
public final class MechanizedInfantryAdvance extends TechAdvance {
  private static final long serialVersionUID = 3040670614877450791L;

  public MechanizedInfantryAdvance(final GameData data) {
    super(TECH_NAME_MECHANIZED_INFANTRY, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_MECHANIZED_INFANTRY;
  }

  @Override
  public void perform(final GamePlayer gamePlayer, final IDelegateBridge bridge) {}

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getMechanizedInfantry();
  }
}
