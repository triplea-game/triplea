package games.strategy.triplea.ai.pro.data;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProTerritoryManagerContestedExitTest {
  private final GameData data = TestMapGameData.WW2V3_1941.getGameData();
  private final Territory karelia = territory("Karelia S.S.R.", data);
  private final Territory inland = territory("Russia", data);
  private final Unit tank = armour(data).create(germans(data));

  private Set<Territory> tankDestinations() {
    data.getSequence().setRoundAndStep(1, "germanCombatMove", germans(data));
    territory("Archangel", data).getUnitCollection().clear();
    addTo(karelia, List.of(tank));
    final ProData proData = new ProData(false);
    proData.initializeSimulation(null, data, germans(data));
    final ProTerritoryManager manager = new ProTerritoryManager(null, proData);
    manager.populateAttackOptions();
    return manager.getAttackOptions().getUnitMoveMap().get(tank);
  }

  @Test
  void tankInContestedEnemyTerritoryCannotAttackFurtherInland() {
    assertThat(tankDestinations()).doesNotContain(inland);
  }

  @Test
  void tankInEmptyEnemyTerritoryCanBlitzFurtherInland() {
    karelia.getUnitCollection().clear();

    assertThat(tankDestinations()).contains(inland);
  }
}
