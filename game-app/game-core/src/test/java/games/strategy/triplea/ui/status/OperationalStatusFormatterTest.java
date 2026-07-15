package games.strategy.triplea.ui.status;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import java.util.List;
import org.junit.jupiter.api.Test;

class OperationalStatusFormatterTest {
  @Test
  void formatsRoadSupplyAndContestedAirspace() throws Exception {
    final GameData data = new GameData();
    final GamePlayer blue = new GamePlayer("Blue", data);
    data.getPlayerList().addPlayerId(blue);
    data.getRelationshipTracker().setSelfRelations();
    data.getRelationshipTracker().setNullPlayerRelations();
    final Territory depot = new Territory("Depot", data);
    final Territory road = new Territory("Road", data);
    depot.setOwner(blue);
    road.setOwner(blue);
    data.getMap().addTerritory(depot);
    data.getMap().addTerritory(road);
    final SupplyTerritoryAttachment depotSupply =
        new SupplyTerritoryAttachment("supplyAttachment", depot, data);
    depot.addAttachment("supplyAttachment", depotSupply);
    depotSupply.setSupplySource("true");
    depotSupply.setRoadConnection("Road");
    road.addAttachment(
        "supplyAttachment", new SupplyTerritoryAttachment("supplyAttachment", road, data));
    data.getProperties().set(SupplyNetworkResolver.SUPPLY_NETWORK_ENABLED, true);
    data.getProperties().set(AirControlTracker.AIR_CONTROL_ENABLED, true);
    data.performChange(AirControlTracker.changeContested(depot, data));

    final String tooltip = OperationalStatusFormatter.territoryTooltip(depot, data, List.of(blue));

    assertThat(tooltip)
        .contains("Supply source: yes")
        .contains("Current road supply: connected")
        .contains("Road links: Road")
        .contains("Air control: contested")
        .contains("Friendly ground attack bonus: +0");
  }
}
