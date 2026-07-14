package games.strategy.triplea.delegate.visibility;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VisibilityServiceTest {
  private final GameData data = new GameData();
  private final GamePlayer blue = new GamePlayer("Blue", data);
  private final GamePlayer red = new GamePlayer("Red", data);
  private final Territory alpha = new Territory("Alpha", data);
  private final Territory bravo = new Territory("Bravo", data);
  private final Territory charlie = new Territory("Charlie", data);
  private final Territory delta = new Territory("Delta", data);
  private final UnitType scout = new UnitType("scout", data);

  @BeforeEach
  void setUp() {
    data.getPlayerList().addPlayerId(blue);
    data.getPlayerList().addPlayerId(red);
    for (final Territory territory : new Territory[] {alpha, bravo, charlie, delta}) {
      data.getMap().addTerritory(territory);
    }
    data.getMap().addConnection(alpha, bravo);
    data.getMap().addConnection(bravo, charlie);
    data.getMap().addConnection(charlie, delta);
    alpha.setOwner(blue);
    bravo.setOwner(red);
    charlie.setOwner(red);
    delta.setOwner(red);
    scout.addAttachment(
        Constants.UNIT_ATTACHMENT_NAME,
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, scout, data));
    data.getUnitTypeList().addUnitType(scout);
    data.getProperties().set(VisibilityService.FOG_OF_WAR_ENABLED, true);
  }

  @Test
  void usesOneTerritoryDefaultRadiusFromFriendlyControlledTerritories() {
    assertThat(VisibilityService.getVisibleTerritories(blue, data)).containsExactly(alpha, bravo);
  }

  @Test
  void friendlyUnitsCreateAdditionalVisionOrigins() {
    final Unit forwardScout = scout.create(1, blue).getFirst();
    charlie.getUnitCollection().add(forwardScout);

    assertThat(VisibilityService.getVisibleTerritories(blue, data))
        .containsExactly(alpha, bravo, charlie, delta);
  }

  @Test
  void enemyUnitsDoNotCreateVisionOrigins() {
    delta.getUnitCollection().add(scout.create(1, red).getFirst());

    assertThat(VisibilityService.getVisibleTerritories(blue, data)).containsExactly(alpha, bravo);
  }

  @Test
  void supportsZeroRadiusAndLegacyDisabledBehavior() {
    data.getProperties().set(VisibilityService.FOG_OF_WAR_VISION_RADIUS, 0);
    assertThat(VisibilityService.getVisibleTerritories(blue, data)).containsExactly(alpha);

    data.getProperties().set(VisibilityService.FOG_OF_WAR_ENABLED, false);
    assertThat(VisibilityService.getVisibleTerritories(blue, data))
        .containsExactly(alpha, bravo, charlie, delta);
  }
}
