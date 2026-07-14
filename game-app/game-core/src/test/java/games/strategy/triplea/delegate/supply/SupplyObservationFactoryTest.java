package games.strategy.triplea.delegate.supply;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import org.junit.jupiter.api.Test;

class SupplyObservationFactoryTest {
  @Test
  void exposesTopologyReachabilityAndUnitCounters() throws Exception {
    final GameData data = new GameData();
    final GamePlayer player = new GamePlayer("Blue", data);
    data.getPlayerList().addPlayerId(player);
    final Territory depot = new Territory("Depot", data);
    final Territory isolated = new Territory("Isolated", data);
    depot.setOwner(player);
    isolated.setOwner(player);
    data.getMap().addTerritory(isolated);
    data.getMap().addTerritory(depot);
    final SupplyTerritoryAttachment depotSupply =
        new SupplyTerritoryAttachment("supplyAttachment", depot, data);
    depot.addAttachment("supplyAttachment", depotSupply);
    depotSupply.setSupplySource("true");
    final UnitType infantry = new UnitType("infantry", data);
    infantry.addAttachment(
        Constants.UNIT_ATTACHMENT_NAME,
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, data));
    data.getUnitTypeList().addUnitType(infantry);
    final Unit unit = infantry.create(1, player).getFirst();
    isolated.getUnitCollection().add(unit);
    data.getProperties().set(SupplyNetworkResolver.SUPPLY_NETWORK_ENABLED, true);
    final SupplyTracker tracker = new SupplyTracker();
    tracker.increment(unit);
    tracker.completeRound(player, 1);

    final SupplyObservation observation = SupplyObservationFactory.create(data, player, tracker);

    assertThat(observation.schemaVersion()).isEqualTo(1);
    assertThat(observation.lastProcessedRound()).isEqualTo(1);
    assertThat(observation.territories())
        .extracting(SupplyObservation.TerritoryState::territory)
        .containsExactly("Depot", "Isolated");
    assertThat(observation.territories().getFirst().supplied()).isTrue();
    assertThat(observation.territories().get(1).supplied()).isFalse();
    assertThat(observation.units())
        .containsExactly(
            new SupplyObservation.UnitState(
                unit.getId().toString(), "Isolated", "infantry", false, 1, 1));
  }
}
