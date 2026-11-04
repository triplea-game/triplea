package games.strategy.triplea.ai.pro.data;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProTerritoryManagerUnloadTest {
  private final GameData data = TestMapGameData.WW2V3_1941.getGameData();
  private final Territory baltic = territory("5 Sea Zone", data);
  private final Unit transport = transport(data).create(germans(data));

  private void unloadedInCombatPhase() throws MutableProperty.InvalidValueException {
    final Unit landed = infantry(data).create(germans(data));
    landed
        .getProperty(Unit.PropertyName.UNLOADED_TO)
        .orElseThrow()
        .setValue(territory("Germany", data));
    landed.getProperty(Unit.PropertyName.UNLOADED_IN_COMBAT_PHASE).orElseThrow().setValue(true);
    transport.getProperty(Unit.PropertyName.UNLOADED).orElseThrow().setValue(List.of(landed));
  }

  private List<ProTransport> nonCombatTransports() {
    data.getSequence().setRoundAndStep(1, "Non Combat Move", germans(data));
    addTo(baltic, List.of(transport));
    addTo(territory("Germany", data), infantry(data).create(1, germans(data)));
    final ProData proData = new ProData(false);
    proData.initializeSimulation(null, data, germans(data));
    final ProTerritoryManager manager = new ProTerritoryManager(null, proData);
    manager.populateDefenseOptions(List.of());
    return manager.getDefendOptions().getTransportList();
  }

  private boolean plansToUnload(final List<ProTransport> transports) {
    return transports.stream()
        .filter(t -> t.getTransport().equals(transport))
        .anyMatch(t -> !t.getTransportMap().isEmpty());
  }

  @Test
  void transportPlansToUnloadWhenFresh() {
    assertThat(plansToUnload(nonCombatTransports())).isTrue();
  }

  @Test
  void transportThatUnloadedInCombatDoesNotPlanToUnloadInNonCombat()
      throws MutableProperty.InvalidValueException {
    unloadedInCombatPhase();

    assertThat(plansToUnload(nonCombatTransports())).isFalse();
  }
}
