package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.util.IntegerMap;

/**
 * At the end of the turn collect units, not income.
 */
@MapSupport
public class NoPUPurchaseDelegate extends PurchaseDelegate {
  private boolean isPacific;

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return false;
  }

  @Override
  public void start() {
    super.start();
    isPacific = isPacificTheater();
    final PlayerID player = bridge.getPlayerId();
    final Collection<Territory> territories = getData().getMap().getTerritoriesOwnedBy(player);
    final Collection<Unit> units = getProductionUnits(territories, player);
    final Change productionChange = ChangeFactory.addUnits(player, units);
    final String transcriptText = player.getName() + " builds " + units.size() + " units.";
    bridge.getHistoryWriter().startEvent(transcriptText);
    if (productionChange != null) {
      bridge.addChange(productionChange);
    }
  }

  private Collection<Unit> getProductionUnits(final Collection<Territory> territories, final PlayerID player) {
    final Collection<Unit> productionUnits = new ArrayList<>();
    if (!(isProductionPerXTerritoriesRestricted() || isProductionPerValuedTerritoryRestricted())) {
      return productionUnits;
    }
    IntegerMap<UnitType> productionPerXTerritories = new IntegerMap<>();
    final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
    // if they have no rules attachments, but are calling NoPU purchase, and have the game property
    // isProductionPerValuedTerritoryRestricted, then they want 1 infantry for each territory with PU value > 0
    if (isProductionPerValuedTerritoryRestricted()
        && ((ra == null) || (ra.getProductionPerXTerritories() == null) || (ra.getProductionPerXTerritories().size()
        == 0))) {
      productionPerXTerritories.put(getData().getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INFANTRY), 1);
    } else if (isProductionPerXTerritoriesRestricted()) {
      productionPerXTerritories = ra.getProductionPerXTerritories();
    } else {
      return productionUnits;
    }
    final Collection<UnitType> unitTypes = new ArrayList<>(productionPerXTerritories.keySet());
    for (final UnitType ut : unitTypes) {
      int unitCount = 0;
      final int prodPerXTerrs = productionPerXTerritories.getInt(ut);
      if (isPacific) {
        unitCount += getBurmaRoad(player);
      }
      int terrCount = 0;
      for (final Territory current : territories) {
        if (!isProductionPerValuedTerritoryRestricted()) {
          terrCount++;
        } else {
          if (TerritoryAttachment.getProduction(current) > 0) {
            terrCount++;
          }
        }
      }
      unitCount += terrCount / prodPerXTerrs;
      productionUnits.addAll(getData().getUnitTypeList().getUnitType(ut.getName()).create(unitCount, player));
    }
    return productionUnits;
  }

  private int getBurmaRoad(final PlayerID player) {
    // only for pacific - should equal 4 for extra inf
    int burmaRoadCount = 0;
    for (final Territory current : getData().getMap().getTerritories()) {
      final String terrName = current.getName();
      if ((terrName.equals("Burma") || terrName.equals("India") || terrName.equals("Yunnan")
          || terrName.equals("Szechwan")) && getData().getRelationshipTracker().isAllied(current.getOwner(), player)) {
        ++burmaRoadCount;
      }
    }
    if (burmaRoadCount == 4) {
      return 1;
    }
    return 0;
  }

  private boolean isPacificTheater() {
    return Properties.getPacificTheater(getData());
  }

  private boolean isProductionPerValuedTerritoryRestricted() {
    return Properties.getProductionPerValuedTerritoryRestricted(getData());
  }

  private boolean isProductionPerXTerritoriesRestricted() {
    return Properties.getProductionPerXTerritoriesRestricted(getData());
  }
}
