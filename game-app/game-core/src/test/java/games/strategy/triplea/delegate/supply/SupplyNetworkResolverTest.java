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

  @Test
  void isSuppliedAlwaysSaysNoForGroundTheAskerDoesNotHold() {
    // Supply only spreads over friendly land, so an attack target is never in the supplied set.
    // Scoring an attack with isSupplied therefore penalises every attack there is.
    front.setOwner(red);

    assertThat(SupplyNetworkResolver.isSupplied(front, blue, data)).isFalse();
  }

  @Test
  void wouldBeSuppliedAnswersForGroundTheAskerIsAboutToTake() {
    front.setOwner(red);

    // Road runs Depot -> Road -> Front, and Blue still holds Road, so taking Front keeps it in
    // supply even though Blue does not hold it yet.
    assertThat(SupplyNetworkResolver.wouldBeSupplied(front, blue, data)).isTrue();
  }

  @Test
  void wouldBeSuppliedIsFalseWhenNoRoadReachesTheGround() {
    front.setOwner(red);
    road.setOwner(red);

    // With the road node lost, taking Front strands whatever takes it.
    assertThat(SupplyNetworkResolver.wouldBeSupplied(front, blue, data)).isFalse();
  }

  @Test
  void everywhereWouldBeSuppliedWhenTheNetworkIsOff() {
    data.getProperties().set(SupplyNetworkResolver.SUPPLY_NETWORK_ENABLED, false);
    front.setOwner(red);
    road.setOwner(red);

    assertThat(SupplyNetworkResolver.wouldBeSupplied(front, blue, data)).isTrue();
  }

  private SupplyTerritoryAttachment attach(final Territory territory) {
    final SupplyTerritoryAttachment attachment =
        new SupplyTerritoryAttachment("supplyAttachment", territory, data);
    territory.addAttachment("supplyAttachment", attachment);
    return attachment;
  }
}
