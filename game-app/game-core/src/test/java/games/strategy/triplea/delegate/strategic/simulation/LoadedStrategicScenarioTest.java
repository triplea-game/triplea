package games.strategy.triplea.delegate.strategic.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.MoveDelegate;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class LoadedStrategicScenarioTest {
  @Test
  void performsNormalMoveAndAdvancesThroughCompleteTurnPhases() throws Exception {
    final GameData data = new GameData();
    data.getResourceList().addResource(new Resource(Constants.PUS, data));
    final GamePlayer blue = new GamePlayer("Blue", data);
    data.getPlayerList().addPlayerId(blue);
    data.getRelationshipTracker().setSelfRelations();
    data.getRelationshipTracker().setNullPlayerRelations();

    final Territory home = territory("Home", blue, data);
    final Territory front = territory("Front", blue, data);
    data.getMap().addTerritory(home);
    data.getMap().addTerritory(front);
    data.getMap().addConnection(home, front);

    final UnitType infantry = new UnitType("infantry", data);
    final UnitAttachment attachment =
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, data);
    setIntField(attachment, "movement", 1);
    infantry.addAttachment(Constants.UNIT_ATTACHMENT_NAME, attachment);
    data.getUnitTypeList().addUnitType(infantry);
    final Unit unit = infantry.create(blue);
    home.getUnitCollection().add(unit);

    final MoveDelegate combatMove = new MoveDelegate();
    combatMove.initialize("combatMove", "Combat Move");
    final BattleDelegate battle = new BattleDelegate();
    battle.initialize("battle", "Battle");
    final MoveDelegate redeployment = new MoveDelegate();
    redeployment.initialize("redeployment", "Redeployment");
    data.addDelegate(combatMove);
    data.addDelegate(battle);
    data.addDelegate(redeployment);
    data.getSequence()
        .addStep(
            new GameStep(
                "BlueCombatMove", "Combat Move", blue, combatMove, data, moveProperties(true)));
    data.getSequence()
        .addStep(new GameStep("BlueBattle", "Battle", blue, battle, data, new Properties()));
    data.getSequence()
        .addStep(
            new GameStep(
                "BlueRedeployment",
                "Redeployment",
                blue,
                redeployment,
                data,
                moveProperties(false)));

    final LoadedStrategicScenario scenario = new LoadedStrategicScenario(data, blue, 5, 32);

    assertThat(scenario.observation().phase()).isEqualTo(StrategicPhase.COMBAT_MOVE);
    final StrategicAction move =
        scenario.legalActions().stream()
            .filter(action -> action.type().equals("move"))
            .filter(action -> action.parameters().get("destination").equals("Front"))
            .findFirst()
            .orElseThrow();
    scenario.step(move);
    assertThat(home.getUnitCollection().contains(unit)).isFalse();
    assertThat(front.getUnitCollection().contains(unit)).isTrue();

    scenario.step(endPhase(StrategicPhase.COMBAT_MOVE));
    assertThat(scenario.observation().phase()).isEqualTo(StrategicPhase.AIR_ASSIGNMENT);
    scenario.step(endPhase(StrategicPhase.AIR_ASSIGNMENT));
    assertThat(scenario.observation().phase()).isEqualTo(StrategicPhase.REDEPLOYMENT);
    scenario.step(endPhase(StrategicPhase.REDEPLOYMENT));
    assertThat(scenario.observation().phase()).isEqualTo(StrategicPhase.COMPLETE);
    assertThat(scenario.observation().over()).isTrue();
    assertThat(scenario.legalActions()).isEmpty();
  }

  private static StrategicAction endPhase(final StrategicPhase phase) {
    return new StrategicAction("end_phase", Map.of("phase", phase.name()));
  }

  private static Territory territory(
      final String name, final GamePlayer owner, final GameData data) {
    final Territory territory = new Territory(name, data);
    territory.setOwner(owner);
    territory.addAttachment(
        Constants.TERRITORY_ATTACHMENT_NAME,
        new TerritoryAttachment(Constants.TERRITORY_ATTACHMENT_NAME, territory, data));
    return territory;
  }

  private static Properties moveProperties(final boolean combatMove) {
    final Properties properties = new Properties();
    properties.setProperty(GameStep.PropertyKeys.COMBAT_MOVE, Boolean.toString(combatMove));
    if (!combatMove) {
      properties.setProperty(GameStep.PropertyKeys.NON_COMBAT_MOVE, "true");
    }
    return properties;
  }

  private static void setIntField(final Object target, final String fieldName, final int value)
      throws Exception {
    final Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.setInt(target, value);
  }
}
