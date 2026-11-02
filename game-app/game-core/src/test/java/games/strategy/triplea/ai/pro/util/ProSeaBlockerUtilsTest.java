package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ProSeaBlockerUtilsTest {

  @Test
  void blockingTheOnlyExitCutsTheTransportRouteToTheTarget() {
    final GameData data = TestMapGameData.WW2V3_1941.getGameData();
    final Territory baltic = territory("5 Sea Zone", data);
    final Territory northSea = territory("6 Sea Zone", data);
    final Territory unitedKingdom = territory("United Kingdom", data);
    removeFrom(northSea, northSea.getUnits());

    final Map<Unit, Territory> balticTransports =
        ProSeaBlockerUtils.findEnemyTransportsWithCargo(data, british(data)).entrySet().stream()
            .filter(e -> e.getValue().equals(baltic))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    assertThat(balticTransports).hasSize(1);
    assertThat(ProSeaBlockerUtils.canUnloadAt(data, balticTransports, unitedKingdom, Set.of()))
        .isTrue();
    assertThat(
            ProSeaBlockerUtils.canUnloadAt(data, balticTransports, unitedKingdom, Set.of(northSea)))
        .isFalse();
  }
}
