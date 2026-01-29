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
  private final Map<UnitType, UnitAttachment> unitInfoMap = new HashMap<>();
  private GameData gameData;

  @Override
  protected void gatherDataBeforeWriting(PrintGenerationData printData) {
    gameData = printData.getData();
    for (final UnitType currentType : gameData.getUnitTypeList()) {
      final UnitAttachment currentTypeUnitAttachment = currentType.getUnitAttachment();
      unitInfoMap.put(currentType, currentTypeUnitAttachment);
    }
  }

  @Override
  protected void writeIntoFile(Writer writer) throws IOException {
    writer.append("Unit Information").append(DELIMITER.repeat(20)).append(LINE_SEPARATOR);
    writer
        .append("Unit")
        .append(DELIMITER)
        .append("Cost")
        .append(DELIMITER)
        .append("Movement")
        .append(DELIMITER)
        .append("Attack")
        .append(DELIMITER)
        .append("Defense")
        .append(DELIMITER)
        .append("CanBlitz")
        .append(DELIMITER)
        .append("Artillery?")
        .append(DELIMITER)
        .append("ArtillerySupportable?")
        .append(DELIMITER)
        .append("Can Produce Units?")
        .append(DELIMITER)
        .append("Marine?")
        .append(DELIMITER)
        .append("Transport Cost")
        .append(DELIMITER)
        .append("AA Gun?")
        .append(DELIMITER)
        .append("Air Unit?")
        .append(DELIMITER)
        .append("Strategic Bomber?")
        .append(DELIMITER)
        .append("Carrier Cost")
        .append(DELIMITER)
        .append("Sea Unit?")
        .append(DELIMITER)
        .append("Hit Points?")
        .append(DELIMITER)
        .append("Transport Capacity")
        .append(DELIMITER)
        .append("Carrier Capacity")
        .append(DELIMITER)
        .append("Submarine?")
        .append(DELIMITER)
        .append("Destroyer?")
        .append(LINE_SEPARATOR);
    writeData(writer);
  }

  private void writeData(Writer writer) throws IOException {
    for (final Entry<UnitType, UnitAttachment> entry : unitInfoMap.entrySet()) {
      final UnitType currentType = entry.getKey();
      final UnitAttachment currentAttachment = entry.getValue();
      if (currentType.getName().equals(Constants.UNIT_TYPE_AAGUN)) {
        writer.append(currentType.getName());
      } else {
        writer.append(StringUtils.capitalize(currentType.getName()));
      }
      writer
          .append(DELIMITER)
          .append(Integer.toString(getCostInformation(currentType, gameData)))
          .append(DELIMITER);
      final GamePlayer nullPlayer = gameData.getPlayerList().getNullPlayer();
      writer
          .append(Integer.toString(currentAttachment.getMovement(nullPlayer)))
          .append(DELIMITER)
          .append(Integer.toString(currentAttachment.getAttack(nullPlayer)))
          .append(DELIMITER)
          .append(Integer.toString(currentAttachment.getDefense(nullPlayer)))
          .append(DELIMITER)
          .append((!currentAttachment.getCanBlitz(nullPlayer) ? "-" : "true"))
          .append(DELIMITER)
          .append((!currentAttachment.getArtillery() ? "-" : "true"))
          .append(DELIMITER)
          .append((!currentAttachment.getArtillerySupportable() ? "-" : "true"))
          .append(DELIMITER)
          .append((!currentAttachment.canProduceUnits() ? "-" : "true"))
          .append(DELIMITER)
          .append(
              (currentAttachment.getIsMarine() == 0
                  ? "-"
                  : Integer.toString(currentAttachment.getIsMarine())))
          .append(DELIMITER)
          .append(
              (currentAttachment.getTransportCost() == -1
                  ? "-"
                  : Integer.toString(currentAttachment.getTransportCost())))
          .append(DELIMITER)
          .append((!Matches.unitTypeIsAaForAnything().test(currentType) ? "-" : "true"))
          .append(DELIMITER)
          .append((!currentAttachment.isAir() ? "-" : "true"))
          .append(DELIMITER)
          .append((!currentAttachment.isStrategicBomber() ? "-" : "true"))
          .append(DELIMITER)
          .append(
              (currentAttachment.getCarrierCost() == -1
                  ? "-"
                  : Integer.toString(currentAttachment.getCarrierCost())))
          .append(DELIMITER)
          .append((!currentAttachment.isSea() ? "-" : "true"))
          .append(DELIMITER)
          .append(Integer.toString(currentAttachment.getHitPoints()))
          .append(DELIMITER)
          .append(
              (currentAttachment.getTransportCapacity() == -1
                  ? "-"
                  : Integer.toString(currentAttachment.getTransportCapacity())))
          .append(DELIMITER)
          .append(
              (currentAttachment.getCarrierCapacity() == -1
                  ? "-"
                  : Integer.toString(currentAttachment.getCarrierCapacity())))
          .append(DELIMITER)
          .append(
              (!(currentAttachment.getCanEvade() && currentAttachment.getIsFirstStrike())
                  ? "-"
                  : "true"))
          .append(DELIMITER)
          .append((!currentAttachment.isDestroyer() ? "-" : "true"));
      writer.append(LINE_SEPARATOR);
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
}
