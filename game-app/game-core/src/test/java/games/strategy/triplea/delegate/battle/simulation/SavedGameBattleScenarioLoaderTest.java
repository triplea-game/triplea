package games.strategy.triplea.delegate.battle.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedGameBattleScenarioLoaderTest {
  @TempDir Path temporaryDirectory;

  @Test
  void loadsAndSelectsPendingBattleFromSaveGame() throws Exception {
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
    assertEquals(1, observation.defense().getFirst().count());
    assertEquals(List.of(), scenario.legalActions());
    assertThrows(
        UnsupportedOperationException.class,
        () -> scenario.step(new BattleAction("wait", Map.of())));
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
    territory.addAttachment(
        Constants.TERRITORY_ATTACHMENT_NAME,
        new TerritoryAttachment(Constants.TERRITORY_ATTACHMENT_NAME, territory, gameData));
    gameData.getMap().addTerritory(territory);

    final UnitType infantry = new UnitType("infantry", gameData);
    infantry.addAttachment(
        Constants.UNIT_ATTACHMENT_NAME,
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, gameData));
    gameData.getUnitTypeList().addUnitType(infantry);
    final Unit attackingUnit = infantry.create(attacker);
    final Unit defendingUnit = infantry.create(defender);

    final BattleDelegate battleDelegate = new BattleDelegate();
    battleDelegate.initialize("battle", "Battle");
    gameData.addDelegate(battleDelegate);

    final BattleTracker battleTracker = battleDelegate.getBattleTracker();
    final MustFightBattle battle =
        new MustFightBattle(territory, attacker, gameData, battleTracker);
    battle.setUnits(List.of(defendingUnit), List.of(attackingUnit), List.of(), defender, List.of());
    addPendingBattle(battleTracker, battle);
    return new Fixture(gameData, battle);
  }

  private Path save(final GameData gameData) throws Exception {
    final Path saveGame = temporaryDirectory.resolve("battle.tsvg");
    try (OutputStream output = Files.newOutputStream(saveGame)) {
      GameDataManager.saveGame(output, gameData);
    }
    return saveGame;
  }

  @SuppressWarnings("unchecked")
  private static void addPendingBattle(final BattleTracker battleTracker, final IBattle battle)
      throws Exception {
    final Field pendingBattlesField = BattleTracker.class.getDeclaredField("pendingBattles");
    pendingBattlesField.setAccessible(true);
    ((Set<IBattle>) pendingBattlesField.get(battleTracker)).add(battle);
  }

  private record Fixture(GameData gameData, MustFightBattle battle) {}
}
