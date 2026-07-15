package games.strategy.triplea.delegate.strategic.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import games.strategy.triplea.delegate.visibility.VisibilityService;
import org.junit.jupiter.api.Test;

class StrategicObservationFactoryTest {
  @Test
  void filtersHiddenSupplyAirControlOwnershipAndUnits() throws Exception {
    final GameData data = new GameData();
    final GamePlayer blue = new GamePlayer("Blue", data);
    final GamePlayer red = new GamePlayer("Red", data);
    data.getPlayerList().addPlayerId(blue);
    data.getPlayerList().addPlayerId(red);
    data.getRelationshipTracker().setSelfRelations();
    data.getRelationshipTracker().setNullPlayerRelations();
    data.getRelationshipTracker()
        .setRelationship(blue, red, data.getRelationshipTypeList().getDefaultWarRelationship());

    final Territory home = new Territory("Home", data);
    final Territory border = new Territory("Border", data);
    final Territory hiddenDepot = new Territory("Hidden Depot", data);
    data.getMap().addTerritory(hiddenDepot);
    data.getMap().addTerritory(home);
    data.getMap().addTerritory(border);
    data.getMap().addConnection(home, border);
    data.getMap().addConnection(border, hiddenDepot);
    home.setOwner(blue);
    border.setOwner(red);
    hiddenDepot.setOwner(red);

    final SupplyTerritoryAttachment supply =
        new SupplyTerritoryAttachment("supplyAttachment", hiddenDepot, data);
    hiddenDepot.addAttachment("supplyAttachment", supply);
    supply.setSupplySource("true");

    final UnitType infantry = new UnitType("infantry", data);
    infantry.addAttachment(
        Constants.UNIT_ATTACHMENT_NAME,
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, data));
    data.getUnitTypeList().addUnitType(infantry);
    hiddenDepot.getUnitCollection().add(infantry.create(1, red).getFirst());

    data.getProperties().set(VisibilityService.FOG_OF_WAR_ENABLED, true);
    data.getProperties().set(SupplyNetworkResolver.SUPPLY_NETWORK_ENABLED, true);
    data.getProperties().set(AirControlTracker.AIR_CONTROL_ENABLED, true);
    data.performChange(AirControlTracker.changeControl(hiddenDepot, red, data));

    final StrategicObservation observation =
        StrategicObservationFactory.create(data, blue, 29, StrategicPhase.COMBAT_MOVE, null);

    assertThat(observation.schemaVersion()).isEqualTo(1);
    assertThat(observation.seed()).isEqualTo(29);
    assertThat(observation.decisionDomain()).isEqualTo(StrategicDecisionDomain.STRATEGIC);
    assertThat(observation.territories())
        .extracting(StrategicObservation.TerritoryState::territory)
        .containsExactly("Border", "Hidden Depot", "Home");

    final StrategicObservation.TerritoryState hidden = observation.territories().get(1);
    assertThat(hidden.visible()).isFalse();
    assertThat(hidden.owner()).isNull();
    assertThat(hidden.supplied()).isNull();
    assertThat(hidden.supplySource()).isFalse();
    assertThat(hidden.airControlPlayer()).isNull();
    assertThat(hidden.units()).isEmpty();
    assertThat(hidden.neighbors()).containsExactly("Border");
  }
}
