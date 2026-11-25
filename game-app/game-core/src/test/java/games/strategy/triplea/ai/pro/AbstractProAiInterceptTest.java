package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AbstractProAiInterceptTest {
  private final GameData data = TestMapGameData.GLOBAL1940.getGameData();

  @Test
  void hardBetaLaunchesEveryFighterThatMayIntercept() {
    final Territory unitedKingdom = territory("United Kingdom", data);
    final List<Unit> fighters = unitType("fighter", data).create(3, british(data));

    assertThat(
            new ProAi("Test Name", "Test Player Label", true)
                .selectUnitsQuery(unitedKingdom, fighters, "Select Air to Intercept"))
        .containsExactlyInAnyOrderElementsOf(fighters);
  }

  @Test
  void stockHardLaunchesNoInterceptors() {
    final Territory unitedKingdom = territory("United Kingdom", data);
    final List<Unit> fighters = unitType("fighter", data).create(3, british(data));

    assertThat(
            new ProAi("Test Name", "Test Player Label")
                .selectUnitsQuery(unitedKingdom, fighters, "Select Air to Intercept"))
        .isNull();
  }

  @Test
  void hardBetaLaunchesOnlyAsManyInterceptorsAsAirBasesAllow()
      throws MutableProperty.InvalidValueException {
    final Territory unitedKingdom = territory("United Kingdom", data);
    unitType("fighter", data)
        .getUnitAttachment()
        .getPropertyOrThrow("requiresAirbaseToIntercept")
        .setValue(true);
    unitType("bomber", data)
        .getUnitAttachment()
        .getPropertyOrThrow("requiresAirbaseToIntercept")
        .setValue(true);
    unitType("airfield", data)
        .getUnitAttachment()
        .getPropertyOrThrow("maxInterceptCount")
        .setValue(2);
    final List<Unit> bombers = unitType("bomber", data).create(2, british(data));
    final List<Unit> fighters = unitType("fighter", data).create(2, british(data));
    final List<Unit> possible = new ArrayList<>(bombers);
    possible.addAll(fighters);

    assertThat(
            new ProAi("Test Name", "Test Player Label", true)
                .selectUnitsQuery(unitedKingdom, possible, "Select Air to Intercept"))
        .containsExactlyInAnyOrderElementsOf(fighters);
  }
}
