package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;
import lombok.extern.slf4j.Slf4j;

/** A technology advance that lowers all unit costs by one. */
@Slf4j
public final class IndustrialTechnologyAdvance extends TechAdvance {
  private static final long serialVersionUID = -21252592806022090L;

  public IndustrialTechnologyAdvance(final GameData data) {
    super(TECH_NAME_INDUSTRIAL_TECHNOLOGY, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_INDUSTRIAL_TECHNOLOGY;
  }

  @Override
  public void perform(final GamePlayer gamePlayer, final IDelegateBridge bridge) {
    final ProductionFrontier current = gamePlayer.getProductionFrontier();
    // they already have it
    if (current.getName().endsWith("IndustrialTechnology")) {
      return;
    }
    final String industrialTechName = current.getName() + "IndustrialTechnology";
    final ProductionFrontier advancedTech =
        bridge.getData().getProductionFrontierList().getProductionFrontier(industrialTechName);
    // it doesnt exist, dont crash
    if (advancedTech == null) {
      log.warn("No tech named: " + industrialTechName + " not adding tech");
      return;
    }
    final Change prodChange = ChangeFactory.changeProductionFrontier(gamePlayer, advancedTech);
    bridge.addChange(prodChange);
  }

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getIndustrialTechnology();
  }
}
