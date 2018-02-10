package org.triplea.performance;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.oddsCalculator.ta.ConcurrentOddsCalculator;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;
import games.strategy.triplea.xml.TestMapGameData;

class BattleOddsCalculator {

  @Test
  void battleCalcRuntime() throws Exception {
    PerfTester.builder()
        .maxRunTime(2200)
        .runCount(5)
        .runnable(runnable())
        .runTest();
  }

  private static Runnable runnable() throws Exception {
    final IOddsCalculator calculator = new ConcurrentOddsCalculator("prefix");

    final GameData data = TestMapGameData.TWW.getGameData();
    calculator.setGameData(data);

    final Territory t = new Territory(data.getMap().getTerritories().get(0).getName(), data);

    final int runCount = 50;

    return () -> calculator.setCalculateData(
        data.getPlayerList().getPlayers().get(0),
        data.getPlayerList().getPlayers().get(1),
        t,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        runCount);
  }
}
