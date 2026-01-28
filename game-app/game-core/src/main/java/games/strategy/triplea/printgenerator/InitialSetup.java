package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.history.HistoryNode;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class InitialSetup {

  protected void run(final PrintGenerationData printData, final boolean useOriginalState) {
    if (useOriginalState) {
      final HistoryNode root = (HistoryNode) printData.getData().getHistory().getRoot();
      printData.getData().getHistory().gotoNode(root);
    }
    new UnitInformation().saveToFile(printData);
    for (final GamePlayer currentPlayer : printData.getData().getPlayerList()) {
      new CountryChart(printData.getOutDir(), currentPlayer).saveToFile(printData);
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
