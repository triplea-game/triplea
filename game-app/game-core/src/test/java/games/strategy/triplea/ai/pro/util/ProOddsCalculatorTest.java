package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.odds.calculator.IBattleCalculator;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ProOddsCalculatorTest {
  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  @Test
  void retreatAfterRoundIsSetForTheSimulationThenCleared() {
    final IBattleCalculator battleCalculator = mock(IBattleCalculator.class);
    when(battleCalculator.calculate(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt()))
        .thenReturn(mock(AggregateResults.class));
    final Territory karelia = territory("Karelia S.S.R.", gameData);
    final List<Unit> attackers = infantry(gameData).create(2, germans(gameData));
    final List<Unit> defenders = infantry(gameData).create(1, russians(gameData));

    new ProOddsCalculator(battleCalculator)
        .callBattleCalcRetreatAfterRound(
            new ProData(), karelia, attackers, defenders, List.of(), 2);

    final InOrder inOrder = inOrder(battleCalculator);
    inOrder.verify(battleCalculator).setRetreatAfterRound(2);
    inOrder
        .verify(battleCalculator)
        .calculate(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt());
    inOrder.verify(battleCalculator).setRetreatAfterRound(-1);
  }
}
