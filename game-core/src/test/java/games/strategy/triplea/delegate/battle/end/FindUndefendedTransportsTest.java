package games.strategy.triplea.delegate.battle.end;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FindUndefendedTransportsTest {

  private static final GameData GLOBAL_1940_GAME_DATA = TestMapGameData.GLOBAL1940.getGameData();
  private static final GamePlayer BRITISH = GameDataTestUtil.british(GLOBAL_1940_GAME_DATA);
  private static final GamePlayer GERMANS = GameDataTestUtil.germans(GLOBAL_1940_GAME_DATA);
  private static final GamePlayer FRENCH = GameDataTestUtil.french(GLOBAL_1940_GAME_DATA);
  private static final Territory SEA_ZONE =
      GameDataTestUtil.territory("1 Sea Zone", GLOBAL_1940_GAME_DATA);

  private Collection<Unit> originalUnitCollection;

  @BeforeEach
  void saveOriginalUnits() {
    originalUnitCollection = SEA_ZONE.getUnitCollection();
  }

  @AfterEach
  void resetOriginalUnits() {
    SEA_ZONE.getUnitCollection().clear();
    SEA_ZONE.getUnitCollection().addAll(originalUnitCollection);
  }

  @Test
  @DisplayName("Verify attacker can retreat if there are available territories")
  void attackerHasRetreat() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.transport(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    SEA_ZONE.getUnitCollection().addAll(friendlyUnits);

    final FindUndefendedTransports.Result result =
        FindUndefendedTransports.builder()
            .player(BRITISH)
            .friendlyUnits(friendlyUnits)
            .isAttacker(true)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(true)
            .gameData(GLOBAL_1940_GAME_DATA)
            .build()
            .find();

    assertThat(result.getTransports(), is(List.of()));
    assertThat(result.getEnemyUnits(), is(List.of()));
  }

  @Test
  @DisplayName(
      "Verify attacker can retreat if an air unit is available, even if it has no attack or support")
  void attackerHasAirRetreat() {
    final GameData gameData = TestMapGameData.PACT_OF_STEEL_2.getGameData();
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final List<Unit> friendlyUnits = GameDataTestUtil.transport(gameData).create(1, british);
    final Unit airTransport = GameDataTestUtil.airTransport(gameData).create(british);
    friendlyUnits.add(airTransport);
    final List<Unit> enemyUnits = GameDataTestUtil.battleship(gameData).create(1, germans);

    final Territory seaZone = GameDataTestUtil.territory("1 Sea Zone", gameData);
    seaZone.getUnitCollection().addAll(friendlyUnits);
    seaZone.getUnitCollection().addAll(enemyUnits);

    // this test requires the airTransport to have attack 0 and to not support anybody
    // so assertThat it doesn't change
    final UnitAttachment ua = UnitAttachment.get(airTransport.getType());
    assertThat(ua.getAttack(british), is(0));
    assertThat(UnitSupportAttachment.get(airTransport.getType()).size(), is(0));

    final FindUndefendedTransports.Result result =
        FindUndefendedTransports.builder()
            .player(BRITISH)
            .friendlyUnits(friendlyUnits)
            .isAttacker(true)
            .battleSite(seaZone)
            .hasRetreatTerritories(false)
            .gameData(gameData)
            .build()
            .find();

    assertThat(result.getTransports(), is(List.of()));
    assertThat(result.getEnemyUnits(), is(List.of()));
  }

  @Test
  @DisplayName("Verify attacker looses transports and enemies are found")
  void attackerHasUndefendedTransports() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.transport(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    SEA_ZONE.getUnitCollection().addAll(friendlyUnits);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);
    SEA_ZONE.getUnitCollection().addAll(enemyUnits);

    final FindUndefendedTransports.Result result =
        FindUndefendedTransports.builder()
            .player(BRITISH)
            .friendlyUnits(friendlyUnits)
            .isAttacker(true)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(false)
            .gameData(GLOBAL_1940_GAME_DATA)
            .build()
            .find();

    assertThat(result.getTransports(), is(friendlyUnits));
    assertThat(result.getEnemyUnits(), is(enemyUnits));
  }

  @Test
  @DisplayName("Verify attacker's ally looses transports and enemies are found")
  void attackersAllyHasUndefendedTransports() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.transport(GLOBAL_1940_GAME_DATA).create(1, FRENCH);
    SEA_ZONE.getUnitCollection().addAll(friendlyUnits);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);
    SEA_ZONE.getUnitCollection().addAll(enemyUnits);

    final FindUndefendedTransports.Result result =
        FindUndefendedTransports.builder()
            .player(BRITISH)
            .friendlyUnits(friendlyUnits)
            .isAttacker(true)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(false)
            .gameData(GLOBAL_1940_GAME_DATA)
            .build()
            .find();

    assertThat(result.getTransports(), is(friendlyUnits));
    assertThat(result.getEnemyUnits(), is(enemyUnits));
  }

  @Test
  @DisplayName("Verify attacker does not lose transports if other ships are around")
  void attackerHasNoUndefendedTransports() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.transport(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    friendlyUnits.addAll(GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, BRITISH));
    SEA_ZONE.getUnitCollection().addAll(friendlyUnits);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);
    SEA_ZONE.getUnitCollection().addAll(enemyUnits);

    final FindUndefendedTransports.Result result =
        FindUndefendedTransports.builder()
            .player(BRITISH)
            .friendlyUnits(friendlyUnits)
            .isAttacker(true)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(false)
            .gameData(GLOBAL_1940_GAME_DATA)
            .build()
            .find();

    assertThat(result.getTransports(), is(List.of()));
    assertThat(result.getEnemyUnits(), is(List.of()));
  }

  @Test
  @DisplayName("Verify nothing is found if attacker has no transports")
  void attackerHasNoTransports() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    SEA_ZONE.getUnitCollection().addAll(friendlyUnits);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);
    SEA_ZONE.getUnitCollection().addAll(enemyUnits);

    final FindUndefendedTransports.Result result =
        FindUndefendedTransports.builder()
            .player(BRITISH)
            .friendlyUnits(friendlyUnits)
            .isAttacker(true)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(false)
            .gameData(GLOBAL_1940_GAME_DATA)
            .build()
            .find();

    assertThat(result.getTransports(), is(List.of()));
    assertThat(result.getEnemyUnits(), is(List.of()));
  }

  @Test
  @DisplayName("Verify defender ignores retreat option")
  void defenderIgnoresRetreat() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.transport(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    SEA_ZONE.getUnitCollection().addAll(friendlyUnits);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);
    SEA_ZONE.getUnitCollection().addAll(enemyUnits);

    final FindUndefendedTransports.Result result =
        FindUndefendedTransports.builder()
            .player(BRITISH)
            .friendlyUnits(friendlyUnits)
            .isAttacker(false)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(true)
            .gameData(GLOBAL_1940_GAME_DATA)
            .build()
            .find();

    assertThat(result.getTransports(), is(friendlyUnits));
    assertThat(result.getEnemyUnits(), is(enemyUnits));
  }

  @Test
  @DisplayName("Verify defender looses transports and enemies are found")
  void defenderHasUndefendedTransports() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.transport(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    SEA_ZONE.getUnitCollection().addAll(friendlyUnits);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);
    SEA_ZONE.getUnitCollection().addAll(enemyUnits);

    final FindUndefendedTransports.Result result =
        FindUndefendedTransports.builder()
            .player(BRITISH)
            .friendlyUnits(friendlyUnits)
            .isAttacker(false)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(false)
            .gameData(GLOBAL_1940_GAME_DATA)
            .build()
            .find();

    assertThat(result.getTransports(), is(friendlyUnits));
    assertThat(result.getEnemyUnits(), is(enemyUnits));
  }

  @Test
  @DisplayName("Verify attacker's ally looses transports and enemies are found")
  void defendersAllyHasUndefendedTransports() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.transport(GLOBAL_1940_GAME_DATA).create(1, FRENCH);
    SEA_ZONE.getUnitCollection().addAll(friendlyUnits);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);
    SEA_ZONE.getUnitCollection().addAll(enemyUnits);

    final FindUndefendedTransports.Result result =
        FindUndefendedTransports.builder()
            .player(BRITISH)
            .friendlyUnits(friendlyUnits)
            .isAttacker(false)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(false)
            .gameData(GLOBAL_1940_GAME_DATA)
            .build()
            .find();

    assertThat(result.getTransports(), is(friendlyUnits));
    assertThat(result.getEnemyUnits(), is(enemyUnits));
  }

  @Test
  @DisplayName("Verify attacker does not lose transports if other ships are around")
  void defenderHasNoUndefendedTransports() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.transport(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    friendlyUnits.addAll(GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, BRITISH));
    SEA_ZONE.getUnitCollection().addAll(friendlyUnits);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);
    SEA_ZONE.getUnitCollection().addAll(enemyUnits);

    final FindUndefendedTransports.Result result =
        FindUndefendedTransports.builder()
            .player(BRITISH)
            .friendlyUnits(friendlyUnits)
            .isAttacker(false)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(false)
            .gameData(GLOBAL_1940_GAME_DATA)
            .build()
            .find();

    assertThat(result.getTransports(), is(List.of()));
    assertThat(result.getEnemyUnits(), is(List.of()));
  }

  @Test
  @DisplayName("Verify nothing is found if attacker has no transports")
  void defenderHasNoTransports() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    SEA_ZONE.getUnitCollection().addAll(friendlyUnits);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);
    SEA_ZONE.getUnitCollection().addAll(enemyUnits);

    final FindUndefendedTransports.Result result =
        FindUndefendedTransports.builder()
            .player(BRITISH)
            .friendlyUnits(friendlyUnits)
            .isAttacker(false)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(false)
            .gameData(GLOBAL_1940_GAME_DATA)
            .build()
            .find();

    assertThat(result.getTransports(), is(List.of()));
    assertThat(result.getEnemyUnits(), is(List.of()));
  }
}
