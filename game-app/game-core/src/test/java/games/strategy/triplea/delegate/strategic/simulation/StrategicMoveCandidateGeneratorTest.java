package games.strategy.triplea.delegate.strategic.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import games.strategy.triplea.delegate.visibility.VisibilityService;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;

class StrategicMoveCandidateGeneratorTest {
  @Test
  void exposesHiddenAdjacentMoveAsUncertainWithoutLeakingDestinationState() throws Exception {
    final Fixture fixture = createFixture();

    final List<StrategicAction> actions =
        StrategicMoveCandidateGenerator.generate(
            fixture.data(), fixture.blue(), StrategicPhase.COMBAT_MOVE, 8);

    assertThat(actions).hasSize(2);
    final StrategicAction move =
        actions.stream().filter(action -> action.type().equals("move")).findFirst().orElseThrow();
    assertThat(move.parameters().get("origin")).isEqualTo("Home");
    assertThat(move.parameters().get("destination")).isEqualTo("Unknown");
    assertThat(move.parameters().get("uncertain")).isEqualTo("true");
    assertThat(move.parameters()).doesNotContainKeys("owner", "enemyUnits", "battleType");
  }

  @Test
  void removesOutOfSupplyLandMovesImmediately() throws Exception {
    final Fixture fixture = createFixture();
    fixture.data().getProperties().set(SupplyNetworkResolver.SUPPLY_NETWORK_ENABLED, true);

    final List<StrategicAction> actions =
        StrategicMoveCandidateGenerator.generate(
            fixture.data(), fixture.blue(), StrategicPhase.COMBAT_MOVE, 8);

    assertThat(actions)
        .containsExactly(
            new StrategicAction("end_phase", java.util.Map.of("phase", "COMBAT_MOVE")));
  }

  @Test
  void rejectsActionMasksThatExceedConfiguredBound() throws Exception {
    final Fixture fixture = createFixture();

    assertThrows(
        StrategicActionSpaceOverflow.class,
        () ->
            StrategicMoveCandidateGenerator.generate(
                fixture.data(), fixture.blue(), StrategicPhase.COMBAT_MOVE, 1));
  }

  private static Fixture createFixture() throws Exception {
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
    final Territory unknown = new Territory("Unknown", data);
    home.setOwner(blue);
    unknown.setOwner(red);
    home.addAttachment(
        Constants.TERRITORY_ATTACHMENT_NAME,
        new TerritoryAttachment(Constants.TERRITORY_ATTACHMENT_NAME, home, data));
    unknown.addAttachment(
        Constants.TERRITORY_ATTACHMENT_NAME,
        new TerritoryAttachment(Constants.TERRITORY_ATTACHMENT_NAME, unknown, data));
    data.getMap().addTerritory(home);
    data.getMap().addTerritory(unknown);
    data.getMap().addConnection(home, unknown);

    final UnitType infantry = new UnitType("infantry", data);
    final UnitAttachment attachment =
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, data);
    setIntField(attachment, "movement", 1);
    infantry.addAttachment(Constants.UNIT_ATTACHMENT_NAME, attachment);
    data.getUnitTypeList().addUnitType(infantry);
    final Unit unit = infantry.create(blue);
    home.getUnitCollection().add(unit);

    data.getProperties().set(VisibilityService.FOG_OF_WAR_ENABLED, true);
    data.getProperties().set(VisibilityService.FOG_OF_WAR_VISION_RADIUS, 0);
    return new Fixture(data, blue);
  }

  private static void setIntField(final Object target, final String fieldName, final int value)
      throws Exception {
    final Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.setInt(target, value);
  }

  private record Fixture(GameData data, GamePlayer blue) {}
}
