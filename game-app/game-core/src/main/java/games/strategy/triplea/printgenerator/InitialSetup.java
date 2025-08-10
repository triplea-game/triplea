package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.history.HistoryNode;
import games.strategy.triplea.attachments.UnitAttachment;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class InitialSetup {
  private final Map<UnitType, UnitAttachment> unitInfoMap = new HashMap<>();

  protected void run(final PrintGenerationData printData, final boolean useOriginalState) {
    if (useOriginalState) {
      final HistoryNode root = (HistoryNode) printData.getData().getHistory().getRoot();
      printData.getData().getHistory().gotoNode(root);
    }
    for (final UnitType currentType : printData.getData().getUnitTypeList()) {
      final UnitAttachment currentTypeUnitAttachment = currentType.getUnitAttachment();
      unitInfoMap.put(currentType, currentTypeUnitAttachment);
    }
    new UnitInformation(unitInfoMap).saveToFile(printData);
    for (final GamePlayer currentPlayer : printData.getData().getPlayerList()) {
      new CountryChart().saveToFile(currentPlayer, printData);
    }
    new PuInfo().saveToFile(printData);
    try {
      new PlayerOrder().saveToFile(printData);
      new PuChart(printData).saveToFile();
    } catch (final IOException e) {
      log.error("Failed to save print generation data", e);
    }
  }
}
