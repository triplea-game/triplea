package games.strategy.triplea.attachments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.gameparser.GameParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupplyTerritoryAttachmentTest {
  private final GameData data = new GameData();
  private final Territory depot = new Territory("Depot", data);
  private final Territory front = new Territory("Front", data);
  private final SupplyTerritoryAttachment attachment =
      new SupplyTerritoryAttachment("supplyAttachment", depot, data);

  @BeforeEach
  void setUp() {
    data.getMap().addTerritory(depot);
    data.getMap().addTerritory(front);
    depot.addAttachment("supplyAttachment", attachment);
  }

  @Test
  void parsesSupplySourceAndRepeatedRoadConnections() throws Exception {
    attachment.setSupplySource("true");
    attachment.setRoadConnection("Front");
    attachment.setRoadConnection("Front");

    assertThat(attachment.getSupplySource()).isTrue();
    assertThat(attachment.getRoadConnections()).containsExactly(front);
    assertThat(SupplyTerritoryAttachment.get(depot)).containsSame(attachment);
    assertThat(attachment.getPropertyOrEmpty("supplySource")).isPresent();
    assertThat(attachment.getPropertyOrEmpty("roadConnection")).isPresent();
  }

  @Test
  void rejectsUnknownRoadTargetsAndSelfConnections() {
    assertThatThrownBy(() -> attachment.setRoadConnection("Missing"))
        .isInstanceOf(GameParseException.class);

    assertThatThrownBy(
            () -> {
              attachment.setRoadConnection("Depot");
              attachment.validate(data);
            })
        .isInstanceOf(GameParseException.class);
  }
}
