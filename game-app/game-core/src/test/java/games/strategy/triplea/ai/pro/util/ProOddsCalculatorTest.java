package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.aaGun;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.italians;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.odds.calculator.IBattleCalculator;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

  @Test
  void interceptorsKilledInTheAirBattleAreNotInTheMainBattle() {
    final GameData airData = TestMapGameData.AIR_BATTLE.getGameData();
    final IBattleCalculator battleCalculator = mockCalculator();
    final Territory site = territory("red middle 1", airData);
    final List<Unit> escorts = fighter(airData).create(100, italians(airData));
    final List<Unit> interceptor = fighter(airData).create(1, russians(airData));
    final List<Unit> infantry = infantry(airData).create(1, russians(airData));
    final List<Unit> defenders = new ArrayList<>(interceptor);
    defenders.addAll(infantry);

    new ProOddsCalculator(battleCalculator)
        .callBattleCalc(new ProData(), site, escorts, defenders, List.of());

    final ArgumentCaptor<Collection<Unit>> attacking = ArgumentCaptor.captor();
    final ArgumentCaptor<Collection<Unit>> defending = ArgumentCaptor.captor();
    verify(battleCalculator)
        .calculate(
            any(),
            any(),
            any(),
            attacking.capture(),
            defending.capture(),
            any(),
            any(),
            anyBoolean(),
            anyInt());
    assertThat(defending.getValue()).containsExactlyElementsOf(infantry);
    assertThat(attacking.getValue()).containsExactlyInAnyOrderElementsOf(escorts);
  }

  @Test
  void mainBattleKeepsEveryUnitWhenAirBattlesAreOff() {
    final GameData airData = TestMapGameData.AIR_BATTLE.getGameData();
    airData.getProperties().set(Constants.BATTLES_MAY_BE_PRECEEDED_BY_AIR_BATTLES, false);
    final IBattleCalculator battleCalculator = mockCalculator();
    final Territory site = territory("red middle 1", airData);
    final List<Unit> escorts = fighter(airData).create(100, italians(airData));
    final List<Unit> defenders = fighter(airData).create(1, russians(airData));

    new ProOddsCalculator(battleCalculator)
        .callBattleCalc(new ProData(), site, escorts, defenders, List.of());

    final ArgumentCaptor<Collection<Unit>> defending = ArgumentCaptor.captor();
    verify(battleCalculator)
        .calculate(
            any(), any(), any(), any(), defending.capture(), any(), any(), anyBoolean(), anyInt());
    assertThat(defending.getValue()).containsExactlyElementsOf(defenders);
  }

  @Test
  void infrastructureOnlyDefendersStillGoToTheCalculatorWhenThereIsNoAirBattle() {
    final IBattleCalculator battleCalculator = mockCalculator();
    final Territory karelia = territory("Karelia S.S.R.", gameData);
    final List<Unit> attackers = infantry(gameData).create(2, germans(gameData));
    final List<Unit> defenders = aaGun(gameData).create(1, russians(gameData));

    new ProOddsCalculator(battleCalculator)
        .callBattleCalc(new ProData(), karelia, attackers, defenders, List.of());

    verify(battleCalculator)
        .calculate(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt());
  }

  @Test
  void escortsWipedOutInTheAirBattleLoseWithoutAMainBattle() {
    final GameData airData = TestMapGameData.AIR_BATTLE.getGameData();
    final IBattleCalculator battleCalculator = mockCalculator();
    final Territory site = territory("red middle 1", airData);
    final List<Unit> escorts = fighter(airData).create(1, italians(airData));
    final List<Unit> defenders = fighter(airData).create(100, russians(airData));

    final ProBattleResult result =
        new ProOddsCalculator(battleCalculator)
            .callBattleCalc(new ProData(), site, escorts, defenders, List.of());

    verify(battleCalculator, never())
        .calculate(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt());
    assertThat(result.getWinPercentage()).isEqualTo(0);
    assertThat(result.getAverageAttackersRemaining()).isEmpty();
  }

  private static IBattleCalculator mockCalculator() {
    final IBattleCalculator battleCalculator = mock(IBattleCalculator.class);
    when(battleCalculator.calculate(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt()))
        .thenReturn(mock(AggregateResults.class));
    return battleCalculator;
  }
}
