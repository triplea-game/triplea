package games.strategy.triplea.delegate.visibility;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import org.junit.jupiter.api.Test;

class VisibilityObservationFactoryTest {
  @Test
  void removesHiddenOwnersAndUnitsWhileKeepingPublicTopology() {
    final GameData data = new GameData();
    final GamePlayer blue = new GamePlayer("Blue", data);
    final GamePlayer red = new GamePlayer("Red", data);
    data.getPlayerList().addPlayerId(blue);
    data.getPlayerList().addPlayerId(red);
    final Territory alpha = new Territory("Alpha", data);
    final Territory bravo = new Territory("Bravo", data);
    final Territory charlie = new Territory("Charlie", data);
    data.getMap().addTerritory(charlie);
    data.getMap().addTerritory(alpha);
    data.getMap().addTerritory(bravo);
    data.getMap().addConnection(alpha, bravo);
    data.getMap().addConnection(bravo, charlie);
    alpha.setOwner(blue);
    bravo.setOwner(red);
    charlie.setOwner(red);
    final UnitType infantry = new UnitType("infantry", data);
    infantry.addAttachment(
        Constants.UNIT_ATTACHMENT_NAME,
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, data));
    data.getUnitTypeList().addUnitType(infantry);
    bravo.getUnitCollection().add(infantry.create(2, red).getFirst());
    bravo.getUnitCollection().add(infantry.create(1, red).getFirst());
    charlie.getUnitCollection().add(infantry.create(1, red).getFirst());
    data.getProperties().set(VisibilityService.FOG_OF_WAR_ENABLED, true);

    final VisibilityObservation observation = VisibilityObservationFactory.create(data, blue);

    assertThat(observation.schemaVersion()).isEqualTo(1);
    assertThat(observation.viewer()).isEqualTo("Blue");
    assertThat(observation.visionRadius()).isEqualTo(1);
    assertThat(observation.territories())
        .extracting(VisibilityObservation.TerritoryState::territory)
        .containsExactly("Alpha", "Bravo", "Charlie");
    assertThat(observation.territories().get(1).visible()).isTrue();
    assertThat(observation.territories().get(1).owner()).isEqualTo("Red");
    assertThat(observation.territories().get(1).units())
        .containsExactly(new VisibilityObservation.UnitGroup("Red", "infantry", 2));
    assertThat(observation.territories().get(2).visible()).isFalse();
    assertThat(observation.territories().get(2).owner()).isNull();
    assertThat(observation.territories().get(2).neighbors()).containsExactly("Bravo");
    assertThat(observation.territories().get(2).units()).isEmpty();
  }
}
