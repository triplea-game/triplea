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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.triplea.java.StringUtils;
import org.triplea.java.collections.CollectionUtils;

class UnitInformation extends InfoForFile {
  final TuvCostsCalculator tuvCalculator = new TuvCostsCalculator();
  private Map<UnitType, UnitAttachment> unitInfoMap = new HashMap<>();
  private GameData gameData;

  @Override
  protected void gatherDataBeforeWriting(PrintGenerationData printData) {
    gameData = printData.getData();
    for (final UnitType currentType : printData.getData().getUnitTypeList()) {
      final UnitAttachment currentTypeUnitAttachment = currentType.getUnitAttachment();
      unitInfoMap.put(currentType, currentTypeUnitAttachment);
    }
  }

  @Override
  protected void writeIntoFile(Writer writer) throws IOException {
    for (int i = 0; i < 8; i++) {
      writer.write(DELIMITER);
    }
    writer.write("Unit Information");
    for (int i = 10; i < 20; i++) {
      writer.write(DELIMITER);
    }
    writer.write(LINE_SEPARATOR);
    writer.write(
        "Unit,Cost,Movement,Attack,Defense,CanBlitz,Artillery?,ArtillerySupportable?,"
            + "Can Produce Units?,Marine?,Transport Cost,AA Gun?,Air Unit?,Strategic Bomber?,"
            + "Carrier Cost,Sea Unit?,Hit Points?,Transport Capacity,Carrier Capacity,"
            + "Submarine?,Destroyer?");
    writer.write(LINE_SEPARATOR);
    writeData(writer);
    writer.write(LINE_SEPARATOR);
  }

  private void writeData(Writer writer) throws IOException {
    for (final Entry<UnitType, UnitAttachment> entry : unitInfoMap.entrySet()) {
      final UnitType currentType = entry.getKey();
      final UnitAttachment currentAttachment = entry.getValue();
      if (currentType.getName().equals(Constants.UNIT_TYPE_AAGUN)) {
        writer.write(currentType.getName() + DELIMITER);
      } else {
        writer.write(StringUtils.capitalize(currentType.getName()) + DELIMITER);
      }
      writer.write(getCostInformation(currentType, gameData) + DELIMITER);
      final GamePlayer nullPlayer = currentType.getData().getPlayerList().getNullPlayer();
      writer.write(
          currentAttachment.getMovement(nullPlayer)
              + DELIMITER
              + currentAttachment.getAttack(nullPlayer)
              + DELIMITER
              + currentAttachment.getDefense(nullPlayer)
              + DELIMITER
              + (!currentAttachment.getCanBlitz(nullPlayer) ? "-" : "true")
              + DELIMITER
              + (!currentAttachment.getArtillery() ? "-" : "true")
              + DELIMITER
              + (!currentAttachment.getArtillerySupportable() ? "-" : "true")
              + DELIMITER
              + (!currentAttachment.canProduceUnits() ? "-" : "true")
              + DELIMITER
              + (currentAttachment.getIsMarine() == 0 ? "-" : currentAttachment.getIsMarine())
              + DELIMITER
              + (currentAttachment.getTransportCost() == -1
                  ? "-"
                  : currentAttachment.getTransportCost())
              + DELIMITER
              + (!Matches.unitTypeIsAaForAnything().test(currentType) ? "-" : "true")
              + DELIMITER
              + (!currentAttachment.isAir() ? "-" : "true")
              + DELIMITER
              + (!currentAttachment.isStrategicBomber() ? "-" : "true")
              + DELIMITER
              + (currentAttachment.getCarrierCost() == -1
                  ? "-"
                  : currentAttachment.getCarrierCost())
              + DELIMITER
              + (!currentAttachment.isSea() ? "-" : "true")
              + DELIMITER
              + currentAttachment.getHitPoints()
              + DELIMITER
              + (currentAttachment.getTransportCapacity() == -1
                  ? "-"
                  : currentAttachment.getTransportCapacity())
              + DELIMITER
              + (currentAttachment.getCarrierCapacity() == -1
                  ? "-"
                  : currentAttachment.getCarrierCapacity())
              + DELIMITER
              + (!(currentAttachment.getCanEvade() && currentAttachment.getIsFirstStrike())
                  ? "-"
                  : "true")
              + DELIMITER
              + (!currentAttachment.isDestroyer() ? "-" : "true"));
      writer.write(LINE_SEPARATOR);
    }
  }

  private int getCostInformation(final UnitType type, final GameData data) {
    final ProductionFrontier production =
        data.getProductionFrontierList().getProductionFrontier(ProductionFrontier.PRODUCTION);
    if (production != null) {
      for (final ProductionRule currentRule : production.getRules()) {
        final NamedAttachable currentType = currentRule.getAnyResultKey();
        if (currentType.equals(type)) {
          return currentRule
              .getCosts()
              .getInt(data.getResourceList().getResourceOrThrow(Constants.PUS));
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

  public static void export(PrintGenerationData printData) {
    new UnitInformation().saveToFile(printData);
  }
}
