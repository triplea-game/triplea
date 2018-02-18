package games.strategy.triplea.delegate;

import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TechAttachment;

/**
 * A technology advance that lowers the cost of building ships.
 */
public final class ImprovedShipyardsAdvance extends TechAdvance {
  private static final long serialVersionUID = 7613381831727736711L;

  public ImprovedShipyardsAdvance(final GameData data) {
    super(TECH_NAME_IMPROVED_SHIPYARDS, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_IMPROVED_SHIPYARDS;
  }

  @Override
  public void perform(final PlayerID id, final IDelegateBridge bridge) {
    final GameData data = bridge.getData();
    if (!Properties.getUseShipyards(data)) {
      return;
    }
    final ProductionFrontier current = id.getProductionFrontier();
    // they already have it
    if (current.getName().endsWith("Shipyards")) {
      return;
    }
    final String industrialTechName = current.getName() + "Shipyards";
    final ProductionFrontier advancedTech = data.getProductionFrontierList().getProductionFrontier(industrialTechName);
    // it doesnt exist, dont crash
    if (advancedTech == null) {
      Logger.getLogger(TechAdvance.class.getName()).log(Level.WARNING,
          "No tech named:" + industrialTechName + " not adding tech");
      return;
    }
    final Change prodChange = ChangeFactory.changeProductionFrontier(id, advancedTech);
    bridge.addChange(prodChange);
  }

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getShipyards();
  }
}
