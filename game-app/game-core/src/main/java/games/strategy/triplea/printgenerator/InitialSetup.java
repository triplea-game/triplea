package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.history.HistoryNode;
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
    new PlayerOrder().saveToFile(printData);
    new PuChart().saveToFile(printData);
  }
}
