package games.strategy.triplea.delegate.battle.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class SavedGameBattleScenarioLoaderTest {
  @TempDir Path temporaryDirectory;

  @Test
  void loadsAndCompletesPendingBattleThroughCasualtyDecision() throws Exception {
    final Fixture fixture = createFixture("Front");
    final Path saveGame = save(fixture.gameData());
    final SavedGameBattleScenarioLoader loader = new SavedGameBattleScenarioLoader();

    final BattleScenario scenario =
        loader.load(
            new BattleResetRequest(
                saveGame.toString(), 91, fixture.battle().getBattleId().toString(), "Front"));
    final BattleObservation observation = scenario.observation();

    assertEquals(BattleObservation.CURRENT_SCHEMA_VERSION, observation.schemaVersion());
    assertEquals(91, observation.seed());
    assertEquals(fixture.battle().getBattleId().toString(), observation.battleId());
    assertEquals("Front", observation.territory());
    assertEquals("attacker", observation.offensePlayer());
    assertEquals("defender", observation.defensePlayer());
    assertEquals(1, observation.offense().getFirst().count());
    assertEquals(2, observation.defense().size());
    assertEquals(BattleDecisionType.SELECT_CASUALTIES, observation.decision().type());
    assertEquals(1, observation.decision().requiredHits());
    assertEquals(2, observation.decision().candidates().size());
    assertEquals("select_casualties", scenario.legalActions().getFirst().type());

    final String casualty = observation.decision().defaultKilledUnitIds().getFirst();
    final BattleScenarioStep step =
        scenario.step(
            new BattleAction(
                "select_casualties", Map.of("damagedUnitIds", "", "killedUnitIds", casualty)));

    assertEquals("SELECT_CASUALTIES", step.info().get("resolvedDecision"));
    assertTrue(scenario.observation().over());
    assertEquals(BattleDecisionType.NONE, scenario.observation().decision().type());
    assertEquals(List.of(), scenario.legalActions());
  }

  @Test
  void rejectsCasualtyActionThatDoesNotAssignRequiredHits() throws Exception {
    final Fixture fixture = createFixture("Front");
    final BattleScenario scenario =
        new SavedGameBattleScenarioLoader()
            .load(new BattleResetRequest(save(fixture.gameData()).toString(), 4));

    assertFalse(
        scenario.isLegalAction(
            new BattleAction(
                "select_casualties", Map.of("damagedUnitIds", "", "killedUnitIds", ""))));
  }

  @Test
  void reportsAvailableBattlesWhenSelectorDoesNotMatch() throws Exception {
    final Fixture fixture = createFixture("Front");
    final Path saveGame = save(fixture.gameData());
    final SavedGameBattleScenarioLoader loader = new SavedGameBattleScenarioLoader();

    final IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                loader.load(
                    new BattleResetRequest(saveGame.toString(), 1, null, "Missing Territory")));

    assertTrue(error.getMessage().contains("available"));
    assertTrue(error.getMessage().contains("Front"));
  }

  private Fixture createFixture(final String territoryName) throws Exception {
    final GameData gameData = new GameData();
    gameData.setGameName("saved battle fixture");
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

    final Territory territory = new Territory(territoryName, gameData);
    territory.setOwner(defender);
    territory.addAttachment(
        Constants.TERRITORY_ATTACHMENT_NAME,
        new TerritoryAttachment(Constants.TERRITORY_ATTACHMENT_NAME, territory, gameData));
    gameData.getMap().addTerritory(territory);

    final UnitType attackingType = addUnitType(gameData, "attackingInfantry", 1, 0);
    final UnitType defendingInfantry = addUnitType(gameData, "defendingInfantry", 0, 0);
    final UnitType defendingArtillery = addUnitType(gameData, "defendingArtillery", 0, 0);
    final Unit attackingUnit = attackingType.create(attacker);
    final Unit defendingUnit = defendingInfantry.create(defender);
    final Unit secondDefendingUnit = defendingArtillery.create(defender);
    territory
        .getUnitCollection()
        .addAll(List.of(attackingUnit, defendingUnit, secondDefendingUnit));

    final BattleDelegate battleDelegate = new BattleDelegate();
    battleDelegate.initialize("battle", "Battle");
    gameData.addDelegate(battleDelegate);
    gameData
        .getSequence()
        .addStep(
            new GameStep(
                "attackerBattle", "Battle", attacker, battleDelegate, gameData, new Properties()));

    final BattleTracker battleTracker = battleDelegate.getBattleTracker();
    final MustFightBattle battle =
        new MustFightBattle(territory, attacker, gameData, battleTracker);
    battle.setUnits(
        List.of(defendingUnit, secondDefendingUnit),
        List.of(attackingUnit),
        List.of(),
        defender,
        List.of());
    addPendingBattle(battleTracker, battle);
    return new Fixture(gameData, battle);
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
    final Path saveGame = temporaryDirectory.resolve("battle.tsvg");
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

  private record Fixture(GameData gameData, MustFightBattle battle) {}
}
