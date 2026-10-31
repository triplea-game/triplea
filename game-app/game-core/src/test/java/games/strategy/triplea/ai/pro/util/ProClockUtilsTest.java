package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProClockUtilsTest {
  private final GameData data = TestMapGameData.REVISED.getGameData();
  private final Territory russia = territory("Russia", data);

  private void emptyRussiaAndMassGermanArmourNextToIt() {
    removeFrom(russia, russia.getMatches(Matches.unitIsNotInfrastructure()));
    addTo(territory("West Russia", data), armour(data).create(50, germans(data)));
  }

  @Test
  void clockIsOneRoundForAnOverwhelmingArmyNextToAnEmptyCapital() {
    emptyRussiaAndMassGermanArmourNextToIt();

    assertThat(ProClockUtils.timeToKill(data, List.of(germans(data)), russia, false, 10))
        .isEqualTo(1);
  }

  @Test
  void clockIsLongerOnTheStartingBoard() {
    assertThat(ProClockUtils.timeToKill(data, List.of(germans(data)), russia, false, 10))
        .isGreaterThan(1);
  }

  @Test
  void eachDefenderAtTheCapitalCutsTheDeficitByTwiceItsHitPointsPlusDefence() {
    emptyRussiaAndMassGermanArmourNextToIt();
    final double before = ProClockUtils.defenceDeficit(data, List.of(germans(data)), russia, 2);

    addTo(russia, infantry(data).create(1, russians(data)));

    // Revised infantry: 1 hit point, defence 2
    assertThat(ProClockUtils.defenceDeficit(data, List.of(germans(data)), russia, 2))
        .isCloseTo(before - 4, within(1e-9));
  }
}
