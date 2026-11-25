package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.makeGameLowLuck;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.util.ProCaptureChainUtils.Army;
import games.strategy.triplea.ai.pro.util.ProCaptureChainUtils.Chain;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.odds.calculator.ConcurrentBattleCalculator;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;

class ProCaptureChainUtilsTest {
  private final GameData gameData = TestMapGameData.LHTR.getGameData();
  private final ProData proData = new ProData(true);
  private final ProOddsCalculator calc;
  private final Territory germany = territory("Germany", gameData);
  private final Territory southernEurope = territory("Southern Europe", gameData);

  ProCaptureChainUtilsTest() {
    ClientSetting.setPreferences(new MemoryPreferences());
    makeGameLowLuck(gameData);
    final ConcurrentBattleCalculator battleCalculator = new ConcurrentBattleCalculator();
    battleCalculator.setGameData(gameData).join();
    calc = new ProOddsCalculator(battleCalculator);
    proData.initializeSimulation(null, gameData, germans(gameData));
  }

  private List<Unit> garrison(final Territory t, final int infantry) {
    removeFrom(t, t.getMatches(Matches.unitIsNotInfrastructure()));
    final List<Unit> defenders = infantry(gameData).create(infantry, germans(gameData));
    addTo(t, defenders);
    return defenders;
  }

  private Army russianArmour(final int count) {
    return new Army(armour(gameData).create(count, russians(gameData)), List.of());
  }

  @Test
  void armyThatLosesStillThinsTheDefendersForTheNext() {
    final List<Unit> defenders = garrison(germany, 8);

    final Chain chain =
        ProCaptureChainUtils.run(
            proData, calc, germany, defenders, List.of(russianArmour(5), russianArmour(4)));

    assertThat(chain.armyCapturePercentages().get(0)).isLessThan(5);
    assertThat(chain.armyCapturePercentages().get(1)).isGreaterThan(90);
    assertThat(chain.capturePercentage()).isGreaterThan(90);
  }

  @Test
  void eachArmyAloneLosesToTheDefenders() {
    final List<Unit> defenders = garrison(germany, 8);

    final Chain first =
        ProCaptureChainUtils.run(proData, calc, germany, defenders, List.of(russianArmour(5)));
    final Chain second =
        ProCaptureChainUtils.run(proData, calc, germany, defenders, List.of(russianArmour(4)));

    assertThat(first.capturePercentage()).isLessThan(5);
    assertThat(second.capturePercentage()).isLessThan(5);
  }

  @Test
  void armiesThatBothLoseEvenAfterTheOtherDoNotCapture() {
    final List<Unit> defenders = garrison(germany, 20);

    final Chain chain =
        ProCaptureChainUtils.run(
            proData, calc, germany, defenders, List.of(russianArmour(3), russianArmour(3)));

    assertThat(chain.capturePercentage()).isLessThan(5);
    assertThat(chain.defendersRemaining()).isNotEmpty();
  }

  @Test
  void combinedAttackNeedsDefenceButEitherArmyAloneDoesNot() {
    final List<Unit> defenders = garrison(germany, 8);

    final Chain both =
        ProCaptureChainUtils.run(
            proData, calc, germany, defenders, List.of(russianArmour(5), russianArmour(4)));
    final Chain alone =
        ProCaptureChainUtils.run(proData, calc, germany, defenders, List.of(russianArmour(4)));

    assertThat(ProCaptureChainUtils.needsDefence(proData, germany, both)).isTrue();
    assertThat(ProCaptureChainUtils.needsDefence(proData, germany, alone)).isFalse();
  }

  @Test
  void capitalLossCountsTheHorizonOfIncomeAndTheFactory() {
    garrison(germany, 1);
    final double income = ProUtils.getPlayerProduction(germans(gameData), gameData);
    final double treasury = ProUtils.capturedTreasury(germany, gameData);

    // Revised factory costs 15
    assertThat(ProCaptureChainUtils.lossValue(proData, germany))
        .isCloseTo(treasury + ProCaptureChainUtils.RECAPTURE_HORIZON * income + 15, within(1e-9));
  }

  @Test
  void factoryThatIsNoCapitalLosesOnlyAWeightedShareOfItsOwnIncomeAndFactory() {
    final double territoryIncome =
        ProCaptureChainUtils.RECAPTURE_HORIZON * TerritoryAttachment.getProduction(southernEurope);

    // Revised factory costs 15
    assertThat(ProCaptureChainUtils.lossValue(proData, southernEurope))
        .isCloseTo(ProCaptureChainUtils.FACTORY_WEIGHT * (territoryIncome + 15), within(1e-9));
    assertThat(ProCaptureChainUtils.lossValue(proData, southernEurope))
        .isLessThan(ProCaptureChainUtils.lossValue(proData, germany));
  }

  @Test
  void territoryWithoutFactoryOrCapitalIsWorthNothing() {
    assertThat(ProCaptureChainUtils.lossValue(proData, territory("Ukraine S.S.R.", gameData)))
        .isZero();
  }

  @Test
  void survivorsAreNotScaledWhenTheArmyNeverCaptures() {
    final List<Unit> starting = garrison(germany, 10);
    final List<Unit> average = starting.subList(0, 4);

    assertThat(ProCaptureChainUtils.survivorsGivenFailure(proData, starting, average, 0))
        .hasSize(4);
  }

  @Test
  void survivorsGivenFailureScaleTheAverageByTheShareOfRunsTheArmyDidNotWin() {
    final List<Unit> starting = garrison(germany, 10);
    final List<Unit> average = starting.subList(0, 4);

    // Half the runs were wins with no survivors, so the other half averaged 8
    assertThat(ProCaptureChainUtils.survivorsGivenFailure(proData, starting, average, 50))
        .hasSize(8);
  }

  @Test
  void survivorsGivenFailureNeverExceedTheStartingDefenders() {
    final List<Unit> starting = garrison(germany, 10);
    final List<Unit> average = starting.subList(0, 4);

    assertThat(ProCaptureChainUtils.survivorsGivenFailure(proData, starting, average, 90))
        .hasSize(10);
  }

  @Test
  void restoredDefendersAreTheMostValuableFallenOnes() {
    final List<Unit> starting = garrison(germany, 2);
    final Unit armour = armour(gameData).create(1, germans(gameData)).get(0);
    starting.add(armour);
    addTo(germany, List.of(armour));
    final List<Unit> average = List.of(starting.get(0));

    assertThat(ProCaptureChainUtils.survivorsGivenFailure(proData, starting, average, 50))
        .containsExactlyInAnyOrder(starting.get(0), armour);
  }

  @Test
  void firstTwoArmiesThatFailAloneStayNearZeroInAThreeArmyChain() {
    final List<Unit> defenders = garrison(germany, 20);

    final Chain chain =
        ProCaptureChainUtils.run(
            proData,
            calc,
            germany,
            defenders,
            List.of(russianArmour(3), russianArmour(3), russianArmour(3)));

    assertThat(chain.armyCapturePercentages().get(0)).isLessThan(5);
    assertThat(chain.armyCapturePercentages().get(1)).isLessThan(5);
    assertThat(chain.capturePercentage()).isLessThan(10);
  }

  @Test
  void territoryOneArmyTakesAloneNeedsNoCombinedDefence() {
    final List<Unit> defenders = garrison(germany, 8);

    final Chain chain =
        ProCaptureChainUtils.run(
            proData, calc, germany, defenders, List.of(russianArmour(12), russianArmour(1)));

    assertThat(chain.strongestAlonePercentage()).isGreaterThan(90);
    assertThat(ProCaptureChainUtils.needsDefence(proData, germany, chain)).isFalse();
  }

  @Test
  void armiesThatOnlyCaptureTogetherLeaveNoArmyStrongAlone() {
    final List<Unit> defenders = garrison(germany, 8);

    final Chain chain =
        ProCaptureChainUtils.run(
            proData, calc, germany, defenders, List.of(russianArmour(5), russianArmour(4)));

    assertThat(chain.strongestAlonePercentage()).isLessThan(5);
  }
}
