package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.util.TuvCostsCalculator;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.StringUtils;
import org.triplea.java.collections.CollectionUtils;

@Slf4j
class UnitInformation {
  final TuvCostsCalculator tuvCalculator = new TuvCostsCalculator();

  void saveToFile(
      final PrintGenerationData printData, final Map<UnitType, UnitAttachment> unitInfoMap) {
    final Path outFile = printData.getOutDir().resolve("General Information.csv");
    try {
      Files.createDirectory(printData.getOutDir());
      try (Writer unitInformation = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
        for (int i = 0; i < 8; i++) {
          unitInformation.write(",");
        }
        unitInformation.write("Unit Information");
        for (int i = 10; i < 20; i++) {
          unitInformation.write(",");
        }
        unitInformation.write("\r\n");
        unitInformation.write(
            "Unit,Cost,Movement,Attack,Defense,CanBlitz,Artillery?,ArtillerySupportable?,"
                + "Can Produce Units?,Marine?,Transport Cost,AA Gun?,Air Unit?,Strategic Bomber?,"
                + "Carrier Cost,Sea Unit?,Hit Points?,Transport Capacity,Carrier Capacity,"
                + "Submarine?,Destroyer?");
        unitInformation.write("\r\n");
        for (final Entry<UnitType, UnitAttachment> entry : unitInfoMap.entrySet()) {
          final UnitType currentType = entry.getKey();
          final UnitAttachment currentAttachment = entry.getValue();
          if (currentType.getName().equals(Constants.UNIT_TYPE_AAGUN)) {
            unitInformation.write(currentType.getName() + ",");
          } else {
            unitInformation.write(StringUtils.capitalize(currentType.getName()) + ",");
          }
          unitInformation.write(getCostInformation(currentType, printData.getData()) + ",");
          final GamePlayer nullPlayer = currentType.getData().getPlayerList().getNullPlayer();
          unitInformation.write(
              currentAttachment.getMovement(nullPlayer)
                  + ","
                  + currentAttachment.getAttack(nullPlayer)
                  + ","
                  + currentAttachment.getDefense(nullPlayer)
                  + ","
                  + (!currentAttachment.getCanBlitz(nullPlayer) ? "-" : "true")
                  + ","
                  + (!currentAttachment.getArtillery() ? "-" : "true")
                  + ","
                  + (!currentAttachment.getArtillerySupportable() ? "-" : "true")
                  + ","
                  + (!currentAttachment.canProduceUnits() ? "-" : "true")
                  + ","
                  + (currentAttachment.getIsMarine() == 0 ? "-" : currentAttachment.getIsMarine())
                  + ","
                  + (currentAttachment.getTransportCost() == -1
                      ? "-"
                      : currentAttachment.getTransportCost())
                  + ","
                  + (!Matches.unitTypeIsAaForAnything().test(currentType) ? "-" : "true")
                  + ","
                  + (!currentAttachment.isAir() ? "-" : "true")
                  + ","
                  + (!currentAttachment.isStrategicBomber() ? "-" : "true")
                  + ","
                  + (currentAttachment.getCarrierCost() == -1
                      ? "-"
                      : currentAttachment.getCarrierCost())
                  + ","
                  + (!currentAttachment.isSea() ? "-" : "true")
                  + ","
                  + currentAttachment.getHitPoints()
                  + ","
                  + (currentAttachment.getTransportCapacity() == -1
                      ? "-"
                      : currentAttachment.getTransportCapacity())
                  + ","
                  + (currentAttachment.getCarrierCapacity() == -1
                      ? "-"
                      : currentAttachment.getCarrierCapacity())
                  + ","
                  + (!(currentAttachment.getCanEvade() && currentAttachment.getIsFirstStrike())
                      ? "-"
                      : "true")
                  + ","
                  + (!currentAttachment.isDestroyer() ? "-" : "true"));
          unitInformation.write("\r\n");
        }
        unitInformation.write("\r\n");
      }
    } catch (final IOException e) {
      log.error("There was an error while trying to save File " + outFile, e);
    }
  }

  private int getCostInformation(final UnitType type, final GameData data) {
    final ProductionFrontier production =
        data.getProductionFrontierList().getProductionFrontier(ProductionFrontier.PRODUCTION);
    if (production != null) {
      for (final ProductionRule currentRule : production.getRules()) {
        final NamedAttachable currentType = currentRule.getAnyResultKey();
        if (currentType.equals(type)) {
          return currentRule.getCosts().getInt(data.getResourceList().getResource(Constants.PUS));
        }
      }
    } else {
      final GamePlayer player = CollectionUtils.getAny(data.getPlayerList().getPlayers());
      final int cost = tuvCalculator.getCostsForTuv(player).getInt(type);
      if (cost > 0) {
        return cost;
      }
    }
    return -1;
  }
}
