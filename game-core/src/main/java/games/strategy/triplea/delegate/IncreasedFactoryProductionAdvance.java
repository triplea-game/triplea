package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/**
 * A technology advance that allows the player to build two more units than the value of the area at
 * a factory, and it also halves the cost of repairing strategic bombing damage.
 */
public final class IncreasedFactoryProductionAdvance extends TechAdvance {
  private static final long serialVersionUID = 987606878563485763L;

  public IncreasedFactoryProductionAdvance(final GameData data) {
    super(TECH_NAME_INCREASED_FACTORY_PRODUCTION, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION;
  }

  @Override
  public void perform(final GamePlayer gamePlayer, final IDelegateBridge bridge) {}

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getIncreasedFactoryProduction();
  }
}
