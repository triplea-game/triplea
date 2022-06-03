package games.strategy.triplea.ai.pro.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.triplea.delegate.GameDataTestUtil.britain;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ProPurchaseUtilsTest {
  final GameState gameData = TestMapGameData.TWW.getGameData();
  final GamePlayer british = checkNotNull(britain(gameData));
  final UnitType trenchType = checkNotNull(unitType("britishEntrenchment", gameData));
  final UnitType materialType = checkNotNull(unitType("Material", gameData));
  final UnitType fortType = checkNotNull(unitType("britishFortification", gameData));
  final Unit trench1 = trenchType.create(british);
  final Unit trench2 = trenchType.create(british);
  final Unit material1 = materialType.create(british);
  final Unit material2 = materialType.create(british);
  final Unit fort1 = fortType.create(british);
  final Unit fort2 = fortType.create(british);

  @Test
  void getUnitsToConsumeBasic() {
    // Fort requires 1 trench and 1 material.
    assertThat(
        getUnitsToConsume(List.of(trench1, material1), List.of(fort1)),
        containsInAnyOrder(trench1, material1));
  }

  @Test
  void getUnitsToConsumeTwoUnits() {
    assertThat(
        getUnitsToConsume(List.of(trench1, trench2, material1, material2), List.of(fort1, fort2)),
        containsInAnyOrder(trench1, trench2, material1, material2));
  }

  @Test
  void getUnitsToConsumeMultipleTypes() {
    // Trench requires 1 material + fort requires 1 trench and 1 material.
    assertThat(
        getUnitsToConsume(List.of(fort2, material1, material2, trench2), List.of(trench1, fort1)),
        containsInAnyOrder(material1, material2, trench2));
  }

  @Test
  void getUnitsToConsumeNotEnough() {
    // An exception should be thrown if insufficient units.
    assertThrows(
        IllegalStateException.class,
        () -> getUnitsToConsume(List.of(material1, material2), List.of(fort1)));
  }

  private Collection<Unit> getUnitsToConsume(Collection<Unit> existing, Collection<Unit> toBuild) {
    return ProPurchaseUtils.getUnitsToConsume(british, existing, toBuild);
  }
}
