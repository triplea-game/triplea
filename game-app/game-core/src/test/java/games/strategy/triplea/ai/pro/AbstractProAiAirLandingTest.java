package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ai.pro.AbstractProAi.LandingAssessment;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AbstractProAiAirLandingTest {
  private final GameData gameData = TestMapGameData.LHTR.getGameData();
  private final Territory unitedKingdom = territory("United Kingdom", gameData);
  private final Territory germany = territory("Germany", gameData);
  private final Territory westernEurope = territory("Western Europe", gameData);

  private Territory choose(final Map<Territory, LandingAssessment> assessments) {
    return AbstractProAi.chooseAirLanding(List.copyOf(assessments.keySet()), assessments::get);
  }

  @Test
  void prefersTheTerritoryLeastLikelyToBeLost() {
    final var chosen =
        choose(
            Map.of(
                germany, new LandingAssessment(20, 20, 50),
                unitedKingdom, new LandingAssessment(0, 0, 5)));

    assertThat(chosen).isEqualTo(unitedKingdom);
  }

  @Test
  void breaksSafetyTiesWithTheMostAlliedGround() {
    final var chosen =
        choose(
            Map.of(
                germany, new LandingAssessment(0, 0, 5),
                unitedKingdom, new LandingAssessment(0, 0, 50)));

    assertThat(chosen).isEqualTo(unitedKingdom);
  }

  @Test
  void landsWhereTheAirIsTheDecidingFactorInTheDefense() {
    final var chosen =
        choose(
            Map.of(
                unitedKingdom, new LandingAssessment(0, 0, 50),
                germany, new LandingAssessment(80, 30, 5)));

    assertThat(chosen).isEqualTo(germany);
  }

  @Test
  void skipsTerritoriesTheAirCannotSave() {
    final var chosen =
        choose(
            Map.of(
                unitedKingdom, new LandingAssessment(0, 0, 50),
                germany, new LandingAssessment(95, 80, 5)));

    assertThat(chosen).isEqualTo(unitedKingdom);
  }

  @Test
  void skipsTerritoriesThatHoldWithoutTheAir() {
    final var chosen =
        choose(
            Map.of(
                unitedKingdom, new LandingAssessment(0, 0, 50),
                germany, new LandingAssessment(10, 5, 5)));

    assertThat(chosen).isEqualTo(unitedKingdom);
  }

  @Test
  void picksTheMostSecureOfSeveralTerritoriesTheAirCouldSave() {
    final var chosen =
        choose(
            Map.of(
                germany, new LandingAssessment(70, 40, 5),
                westernEurope, new LandingAssessment(90, 20, 5),
                unitedKingdom, new LandingAssessment(0, 0, 50)));

    assertThat(chosen).isEqualTo(westernEurope);
  }
}
