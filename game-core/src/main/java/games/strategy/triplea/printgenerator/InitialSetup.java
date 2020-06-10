package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.history.HistoryNode;
import games.strategy.triplea.attachments.UnitAttachment;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import lombok.extern.java.Log;

@Log
class InitialSetup {
  private final Map<UnitType, UnitAttachment> unitInfoMap = new HashMap<>();

  protected void run(final PrintGenerationData printData, final boolean useOriginalState) {
    final GameData gameData = printData.getData();
    if (useOriginalState) {
      final HistoryNode root = (HistoryNode) gameData.getHistory().getRoot();
      gameData.getHistory().gotoNode(root);
    }
    for (final UnitType currentType : gameData.getUnitTypeList()) {
      final UnitAttachment currentTypeUnitAttachment = UnitAttachment.get(currentType);
      unitInfoMap.put(currentType, currentTypeUnitAttachment);
    }
    new UnitInformation().saveToFile(printData, unitInfoMap);
    for (final GamePlayer currentPlayer : gameData.getPlayerList()) {
      new CountryChart().saveToFile(currentPlayer, printData);
    }
    new PuInfo().saveToFile(printData);
    try {
      new PlayerOrder().saveToFile(printData);
      new PuChart(printData).saveToFile();
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Failed to save print generation data", e);
    }
  }
}
