package games.strategy.triplea.printgenerator;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.util.TuvUtils;

class UnitInformation {
  private GameData data;

  private static String capitalizeFirst(final String s) {
    return (s.length() > 0) ? (Character.toUpperCase(s.charAt(0)) + s.substring(1)) : s;
  }

  void saveToFile(final PrintGenerationData printData, final Map<UnitType, UnitAttachment> unitInfoMap) {
    data = printData.getData();
    printData.getOutDir().mkdir();
    final File outFile = new File(printData.getOutDir(), "General Information.csv");
    try (Writer unitInformation = Files.newBufferedWriter(outFile.toPath(), StandardCharsets.UTF_8)) {
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
      for (final Entry<UnitType, UnitAttachment> entry : unitInfoMap.entrySet()) {
        final UnitType currentType = entry.getKey();
        final UnitAttachment currentAttachment = entry.getValue();
        if (currentType.getName().equals(Constants.UNIT_TYPE_AAGUN)) {
          unitInformation.write(currentType.getName() + ",");
        } else {
          unitInformation.write(capitalizeFirst(currentType.getName()) + ",");
        }
        unitInformation.write(getCostInformation(currentType) + ",");
        unitInformation.write(currentAttachment.getMovement(PlayerID.NULL_PLAYERID) + ","
            + currentAttachment.getAttack(PlayerID.NULL_PLAYERID) + ","
            + currentAttachment.getDefense(PlayerID.NULL_PLAYERID) + ","
            + (!currentAttachment.getCanBlitz(PlayerID.NULL_PLAYERID) ? "-" : "true") + ","
            + (!currentAttachment.getArtillery() ? "-" : "true") + ","
            + (!currentAttachment.getArtillerySupportable() ? "-" : "true") + ","
            + (!currentAttachment.getCanProduceUnits() ? "-" : "true") + ","
            + (currentAttachment.getIsMarine() == 0 ? "-" : currentAttachment.getIsMarine()) + ","
            + (currentAttachment.getTransportCost() == -1 ? "-" : currentAttachment.getTransportCost()) + ","
            + (!Matches.unitTypeIsAaForAnything().test(currentType) ? "-" : "true") + ","
            + (!currentAttachment.getIsAir() ? "-" : "true") + ","
            + (!currentAttachment.getIsStrategicBomber() ? "-" : "true") + ","
            + (currentAttachment.getCarrierCost() == -1 ? "-" : currentAttachment.getCarrierCost()) + ","
            + (!currentAttachment.getIsSea() ? "-" : "true") + "," + (currentAttachment.getHitPoints()) + ","
            + (currentAttachment.getTransportCapacity() == -1 ? "-" : currentAttachment.getTransportCapacity()) + ","
            + (currentAttachment.getCarrierCapacity() == -1 ? "-" : currentAttachment.getCarrierCapacity()) + ","
            + (!currentAttachment.getIsSub() ? "-" : "true") + ","
            + (!currentAttachment.getIsDestroyer() ? "-" : "true"));
        unitInformation.write("\r\n");
      }
      unitInformation.write("\r\n");
    } catch (final IOException e) {
      ClientLogger.logError("There was an error while trying to save File " + outFile.toString(), e);
    }
  }

  private int getCostInformation(final UnitType type) {
    if (data.getProductionFrontierList().getProductionFrontier("production") != null) {
      final List<ProductionRule> productionRules =
          data.getProductionFrontierList().getProductionFrontier("production").getRules();
      for (final ProductionRule currentRule : productionRules) {
        final NamedAttachable currentType = currentRule.getResults().keySet().iterator().next();
        if (currentType.equals(type)) {
          return currentRule.getCosts().getInt(data.getResourceList().getResource(Constants.PUS));
        }
      }
    } else {
      if (TuvUtils.getCostsForTuv(data.getPlayerList().getPlayers().iterator().next(), data)
          .getInt(type) > 0) {
        return TuvUtils.getCostsForTuv(data.getPlayerList().getPlayers().iterator().next(), data)
            .getInt(type);
      }
    }
    return -1;
  }
}
