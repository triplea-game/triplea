package games.strategy.triplea.delegate.supply;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupplyNetworkResolverTest {
  private final GameData data = new GameData();
  private final GamePlayer blue = new GamePlayer("Blue", data);
  private final GamePlayer red = new GamePlayer("Red", data);
  private final Territory depot = new Territory("Depot", data);
  private final Territory road = new Territory("Road", data);
  private final Territory front = new Territory("Front", data);

  @BeforeEach
  void setUp() throws Exception {
    data.getPlayerList().addPlayerId(blue);
    data.getPlayerList().addPlayerId(red);
    data.getMap().addTerritory(front);
    data.getMap().addTerritory(road);
    data.getMap().addTerritory(depot);
    depot.setOwner(blue);
    road.setOwner(blue);
    front.setOwner(blue);
    data.getProperties().set(SupplyNetworkResolver.SUPPLY_NETWORK_ENABLED, true);

    final SupplyTerritoryAttachment depotSupply = attach(depot);
    final SupplyTerritoryAttachment roadSupply = attach(road);
    attach(front);
    depotSupply.setSupplySource("true");
    depotSupply.setRoadConnection("Road");
    roadSupply.setRoadConnection("Front");
  }

  @Test
  void reachesFriendlyTerritoriesThroughOneSidedRoadDeclarations() {
    assertThat(SupplyNetworkResolver.getSupplySources(blue, data)).containsExactly(depot);
    assertThat(SupplyNetworkResolver.getRoadNeighbors(road, data)).containsExactly(depot, front);
    assertThat(SupplyNetworkResolver.getSuppliedTerritories(blue, data))
        .containsExactly(depot, road, front);
    assertThat(SupplyNetworkResolver.isSupplied(front, blue, data)).isTrue();
  }

  @Test
  void hostileControlCutsAllTerritoriesBeyondTheRoadNode() {
    road.setOwner(red);

    assertThat(SupplyNetworkResolver.getSuppliedTerritories(blue, data)).containsExactly(depot);
    assertThat(SupplyNetworkResolver.isSupplied(road, blue, data)).isFalse();
    assertThat(SupplyNetworkResolver.isSupplied(front, blue, data)).isFalse();
  }

  private SupplyTerritoryAttachment attach(final Territory territory) {
    final SupplyTerritoryAttachment attachment =
        new SupplyTerritoryAttachment("supplyAttachment", territory, data);
    territory.addAttachment("supplyAttachment", attachment);
    return attachment;
  }
}
