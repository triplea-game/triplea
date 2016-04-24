package games.strategy.triplea.printgenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.Matches;

class UnitInformation {
  private Map<UnitType, UnitAttachment> unitInfoMap;
  private Iterator<UnitType> unitTypeIterator;
  private GameData data;
  private PrintGenerationData printData;

  private static String capitalizeFirst(final String s) {
    return (s.length() > 0) ? Character.toUpperCase(s.charAt(0)) + s.substring(1) : s;
  }

  protected void saveToFile(final PrintGenerationData printData, final Map<UnitType, UnitAttachment> unitInfoMap) {
    FileWriter unitInformation = null;
    this.printData = printData;
    data = this.printData.getData();
    this.unitInfoMap = unitInfoMap;
    unitTypeIterator = this.unitInfoMap.keySet().iterator();
    this.printData.getOutDir().mkdir();
    final File outFile = new File(this.printData.getOutDir(), "General Information.csv");
    try {
      unitInformation = new FileWriter(outFile);
      for (int i = 0; i < 8; i++) {
        unitInformation.write(",");
      }
      unitInformation.write("Unit Information");
      for (int i = 10; i < 20; i++) {
        unitInformation.write(",");
      }
      unitInformation.write("\r\n");
      unitInformation.write("Unit,Cost,Movement,Attack,Defense,CanBlitz,Artillery?,ArtillerySupportable?"
          + ",Can Produce Units?,Marine?,Transport Cost,AA Gun?,Air Unit?,Strategic Bomber?,Carrier Cost,"
          + "Sea Unit?,Hit Points?,Transport Capacity,Carrier Capacity,Submarine?,Destroyer?");
      unitInformation.write("\r\n");
      while (unitTypeIterator.hasNext()) {
        final UnitType currentType = unitTypeIterator.next();
        final UnitAttachment currentAttachment = this.unitInfoMap.get(currentType);
        if (currentType.getName().equals(Constants.AAGUN_TYPE)) {
          unitInformation.write(currentType.getName() + ",");
        } else {
          unitInformation.write(capitalizeFirst(currentType.getName()) + ",");
        }
        unitInformation.write(getCostInformation(currentType) + ",");
        unitInformation.write(currentAttachment.getMovement(PlayerID.NULL_PLAYERID) + ","
            + currentAttachment.getAttack(PlayerID.NULL_PLAYERID) + ","
            + currentAttachment.getDefense(PlayerID.NULL_PLAYERID) + ","
            + (currentAttachment.getCanBlitz(PlayerID.NULL_PLAYERID) == false ? "-" : "true") + ","
            + (currentAttachment.getArtillery() == false ? "-" : "true") + ","
            + (currentAttachment.getArtillerySupportable() == false ? "-" : "true") + ","
            + (currentAttachment.getCanProduceUnits() == false ? "-" : "true") + ","
            + (currentAttachment.getIsMarine() == 0 ? "-" : currentAttachment.getIsMarine()) + ","
            + (currentAttachment.getTransportCost() == -1 ? "-" : currentAttachment.getTransportCost()) + ","
            + (Matches.UnitTypeIsAAforAnything.match(currentType) == false ? "-" : "true") + ","
            + (currentAttachment.getIsAir() == false ? "-" : "true") + ","
            + (currentAttachment.getIsStrategicBomber() == false ? "-" : "true") + ","
            + (currentAttachment.getCarrierCost() == -1 ? "-" : currentAttachment.getCarrierCost()) + ","
            + (currentAttachment.getIsSea() == false ? "-" : "true") + "," + (currentAttachment.getHitPoints()) + ","
            + (currentAttachment.getTransportCapacity() == -1 ? "-" : currentAttachment.getTransportCapacity()) + ","
            + (currentAttachment.getCarrierCapacity() == -1 ? "-" : currentAttachment.getCarrierCapacity()) + ","
            + (currentAttachment.getIsSub() == false ? "-" : "true") + ","
            + (currentAttachment.getIsDestroyer() == false ? "-" : "true"));
        unitInformation.write("\r\n");
      }
      unitInformation.write("\r\n");
      unitInformation.close();
    } catch (final IOException e) {
      ClientLogger.logError("There was an error while trying to save File " + outFile.toString() , e);
    } 
  }

  private int getCostInformation(final UnitType type) {
    if (data.getProductionFrontierList().getProductionFrontier("production") != null) {
      final List<ProductionRule> productionRules =
          data.getProductionFrontierList().getProductionFrontier("production").getRules();
      final Iterator<ProductionRule> productionIterator = productionRules.iterator();
      while (productionIterator.hasNext()) {
        final ProductionRule currentRule = productionIterator.next();
        final NamedAttachable currentType = currentRule.getResults().keySet().iterator().next();
        if (currentType.equals(type)) {
          final int cost = currentRule.getCosts().getInt(data.getResourceList().getResource(Constants.PUS));
          return cost;
        }
      }
    } else {
      if (BattleCalculator.getCostsForTUV(data.getPlayerList().getPlayers().iterator().next(), data)
          .getInt(type) > 0) {
        return BattleCalculator.getCostsForTUV(data.getPlayerList().getPlayers().iterator().next(), data)
            .getInt(type);
      }
    }
    return -1;
  }
}