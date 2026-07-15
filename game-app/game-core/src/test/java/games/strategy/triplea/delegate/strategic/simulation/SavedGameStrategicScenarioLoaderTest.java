package games.strategy.triplea.delegate.strategic.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.simulation.BattleDecisionType;
import games.strategy.triplea.delegate.battle.simulation.BattleObservation;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedGameStrategicScenarioLoaderTest {
  @TempDir Path temporaryDirectory;

  @Test
  void loadsPlayerTurnAndHandsPendingBattleToBattlePolicy() throws Exception {
    final Fixture fixture = createFixture();
    final Path saveGame = save(fixture.gameData());
    final StatefulStrategicEnvironment environment =
        new StatefulStrategicEnvironment(new SavedGameStrategicScenarioLoader());

    final StrategicObservation initial =
        environment.reset(
            new StrategicResetRequest(saveGame.toString(), 73, fixture.attacker().getName()));

    assertThat(initial.phase()).isEqualTo(StrategicPhase.BATTLE);
    assertThat(initial.decisionDomain()).isEqualTo(StrategicDecisionDomain.BATTLE);
    assertThat(initial.battle()).isNotNull();
    assertThat(initial.battle().decision().type()).isEqualTo(BattleDecisionType.SELECT_CASUALTIES);
    assertThat(initial.pendingBattles()).hasSize(1);
    assertThat(environment.legalActions())
        .isNotEmpty()
        .allMatch(action -> action.type().equals("battle_decision"));

    StrategicStepResult result = environment.step(executableBattleAction(initial));
    while (!result.terminated()) {
      result = environment.step(executableBattleAction(result.observation()));
    }

    assertThat(result.observation().phase()).isEqualTo(StrategicPhase.COMPLETE);
    assertThat(result.observation().decisionDomain()).isEqualTo(StrategicDecisionDomain.COMPLETE);
    assertThat(result.observation().battle()).isNull();
    assertThat(result.observation().pendingBattles()).isEmpty();
    assertThat(environment.legalActions()).isEmpty();
  }

  @Test
  void rejectsUnknownPlayerWithAvailableNames() throws Exception {
    final Fixture fixture = createFixture();
    final Path saveGame = save(fixture.gameData());

    final IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new SavedGameStrategicScenarioLoader()
                    .load(new StrategicResetRequest(saveGame.toString(), 1, "Missing")));

    assertThat(error.getMessage()).contains("attacker", "defender");
  }

  private static StrategicAction executableBattleAction(final StrategicObservation observation) {
    final BattleObservation battle = observation.battle();
    if (battle == null) {
      throw new IllegalStateException("expected an active battle observation");
    }
    final Map<String, String> parameters = new java.util.TreeMap<>();
    parameters.put("battleId", battle.battleId());
    parameters.put("territory", battle.territory());
    switch (battle.decision().type()) {
      case SELECT_CASUALTIES -> {
        parameters.put("battleActionType", "select_casualties");
        parameters.put("killedUnitIds", String.join(",", battle.decision().defaultKilledUnitIds()));
        parameters.put(
            "damagedUnitIds", String.join(",", battle.decision().defaultDamagedUnitIds()));
      }
      case RETREAT, SUBMERGE -> parameters.put("battleActionType", "continue");
      case NONE -> throw new IllegalStateException("battle has no pending decision");
    }
    return new StrategicAction("battle_decision", parameters);
  }

  private Fixture createFixture() throws Exception {
    final GameData gameData = new GameData();
    gameData.setGameName("saved strategic fixture");
    gameData.setDiceSides(1);
    gameData.getResourceList().addResource(new Resource(Constants.PUS, gameData));

    final GamePlayer attacker = new GamePlayer("attacker", gameData);
    final GamePlayer defender = new GamePlayer("defender", gameData);
    gameData.getPlayerList().addPlayerId(attacker);
    gameData.getPlayerList().addPlayerId(defender);
    gameData.getRelationshipTracker().setSelfRelations();
    gameData.getRelationshipTracker().setNullPlayerRelations();
    gameData
        .getRelationshipTracker()
        .setRelationship(
            attacker, defender, gameData.getRelationshipTypeList().getDefaultWarRelationship());

    final Territory front = new Territory("Front", gameData);
    front.setOwner(defender);
    front.addAttachment(
        Constants.TERRITORY_ATTACHMENT_NAME,
        new TerritoryAttachment(Constants.TERRITORY_ATTACHMENT_NAME, front, gameData));
    gameData.getMap().addTerritory(front);

    final UnitType attackingType = addUnitType(gameData, "attackingInfantry", 1, 0);
    final UnitType defendingInfantry = addUnitType(gameData, "defendingInfantry", 0, 0);
    final UnitType defendingArtillery = addUnitType(gameData, "defendingArtillery", 0, 0);
    final Unit attackingUnit = attackingType.create(attacker);
    final Unit defendingUnit = defendingInfantry.create(defender);
    final Unit secondDefendingUnit = defendingArtillery.create(defender);
    front.getUnitCollection().addAll(List.of(attackingUnit, defendingUnit, secondDefendingUnit));

    final BattleDelegate battleDelegate = new BattleDelegate();
    battleDelegate.initialize("battle", "Battle");
    gameData.addDelegate(battleDelegate);
    gameData
        .getSequence()
        .addStep(
            new GameStep(
                "attackerBattle", "Battle", attacker, battleDelegate, gameData, new Properties()));

    final BattleTracker battleTracker = battleDelegate.getBattleTracker();
    final MustFightBattle battle = new MustFightBattle(front, attacker, gameData, battleTracker);
    battle.setUnits(
        List.of(defendingUnit, secondDefendingUnit),
        List.of(attackingUnit),
        List.of(),
        defender,
        List.of());
    addPendingBattle(battleTracker, battle);
    return new Fixture(gameData, attacker);
  }

  private static UnitType addUnitType(
      final GameData gameData, final String name, final int attack, final int defense)
      throws Exception {
    final UnitType unitType = new UnitType(name, gameData);
    final UnitAttachment attachment =
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, unitType, gameData);
    setIntField(attachment, "attack", attack);
    setIntField(attachment, "defense", defense);
    unitType.addAttachment(Constants.UNIT_ATTACHMENT_NAME, attachment);
    gameData.getUnitTypeList().addUnitType(unitType);
    return unitType;
  }

  private Path save(final GameData gameData) throws Exception {
    final Path saveGame = temporaryDirectory.resolve("strategic.tsvg");
    try (OutputStream output = Files.newOutputStream(saveGame)) {
      GameDataManager.saveGame(output, gameData);
    }
    return saveGame;
  }

  private static void setIntField(final Object target, final String fieldName, final int value)
      throws Exception {
    final Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.setInt(target, value);
  }

  @SuppressWarnings("unchecked")
  private static void addPendingBattle(final BattleTracker battleTracker, final IBattle battle)
      throws Exception {
    final Field pendingBattlesField = BattleTracker.class.getDeclaredField("pendingBattles");
    pendingBattlesField.setAccessible(true);
    ((Set<IBattle>) pendingBattlesField.get(battleTracker)).add(battle);
    battleTracker
        .getBattleRecords()
        .addBattle(
            battle.getAttacker(),
            battle.getBattleId(),
            battle.getTerritory(),
            battle.getBattleType());
  }

  private record Fixture(GameData gameData, GamePlayer attacker) {}
}
