package games.strategy.triplea.delegate.battle.end;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FindUnitsWithNoRollsLeftTest {

  private static final GameData GLOBAL_1940_GAME_DATA = TestMapGameData.GLOBAL1940.getGameData();
  private static final GamePlayer BRITISH = GameDataTestUtil.british(GLOBAL_1940_GAME_DATA);
  private static final GamePlayer GERMANS = GameDataTestUtil.germans(GLOBAL_1940_GAME_DATA);
  private static final Territory SEA_ZONE =
      GameDataTestUtil.territory("1 Sea Zone", GLOBAL_1940_GAME_DATA);

  @Test
  @DisplayName("Verify attacker looses units if can't retreat")
  void attackerHasNoRetreat() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.transport(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);

    final Collection<Unit> result =
        FindUnitsWithNoRollsLeft.builder()
            .friendlyUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(false)
            .isAttacker(true)
            .build()
            .find();

    assertThat(result, is(friendlyUnits));
  }

  @Test
  @DisplayName("Verify attacker can retreat if there are available territories")
  void attackerHasRetreat() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.transport(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);

    final Collection<Unit> result =
        FindUnitsWithNoRollsLeft.builder()
            .friendlyUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(true)
            .isAttacker(true)
            .build()
            .find();

    assertThat(result, is(List.of()));
  }

  @Test
  @DisplayName(
      "Verify attacker can retreat if an air unit is available, "
          + "even if it has no attack or support")
  void attackerHasAirRetreat() {
    final GameData gameData = TestMapGameData.PACT_OF_STEEL_2.getGameData();
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final List<Unit> friendlyUnits = GameDataTestUtil.transport(gameData).create(1, british);
    final Unit airTransport = GameDataTestUtil.airTransport(gameData).create(british);
    friendlyUnits.add(airTransport);
    final List<Unit> enemyUnits = GameDataTestUtil.battleship(gameData).create(1, germans);
    final Territory seaZone = GameDataTestUtil.territory("1 Sea Zone", gameData);

    // this test requires the airTransport to have attack 0 and to not support anybody
    // so assertThat it doesn't change
    final UnitAttachment ua = UnitAttachment.get(airTransport.getType());
    assertThat(ua.getAttack(british), is(0));
    assertThat(UnitSupportAttachment.get(airTransport.getType()).size(), is(0));

    final Collection<Unit> result =
        FindUnitsWithNoRollsLeft.builder()
            .friendlyUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .battleSite(seaZone)
            .hasRetreatTerritories(false)
            .isAttacker(true)
            .build()
            .find();

    assertThat(result, is(List.of()));
  }

  @Test
  @DisplayName("Verify attacker will not loose units if they can attack")
  void attackerHasUnitsWithRolls() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);

    final Collection<Unit> result =
        FindUnitsWithNoRollsLeft.builder()
            .friendlyUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(false)
            .isAttacker(true)
            .build()
            .find();

    assertThat(result, is(List.of()));
  }

  @Test
  @DisplayName("Verify attacker looses units if subs are submerged")
  void attackerHasSubmergedSubs() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.transport(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    final Unit submergedSub = GameDataTestUtil.submarine(GLOBAL_1940_GAME_DATA).create(BRITISH);
    submergedSub.setSubmerged(true);
    friendlyUnits.add(submergedSub);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);

    final Collection<Unit> result =
        FindUnitsWithNoRollsLeft.builder()
            .friendlyUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(false)
            .isAttacker(true)
            .build()
            .find();

    assertThat(result, is(List.of(friendlyUnits.get(0))));
  }

  @Test
  @DisplayName("Verify defender doesn't care about retreat options")
  void defenderIgnoresRetreat() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.transport(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);

    final Collection<Unit> result =
        FindUnitsWithNoRollsLeft.builder()
            .friendlyUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(true)
            .isAttacker(false)
            .build()
            .find();

    assertThat(result, is(friendlyUnits));
  }

  @Test
  void defenderHasUnitsWithRolls() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);

    final Collection<Unit> result =
        FindUnitsWithNoRollsLeft.builder()
            .friendlyUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(true)
            .isAttacker(false)
            .build()
            .find();

    assertThat(result, is(List.of()));
  }

  @Test
  void defenderHasSubmergedSubs() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.transport(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    final Unit submergedSub = GameDataTestUtil.submarine(GLOBAL_1940_GAME_DATA).create(BRITISH);
    submergedSub.setSubmerged(true);
    friendlyUnits.add(submergedSub);
    final List<Unit> enemyUnits =
        GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS);

    final Collection<Unit> result =
        FindUnitsWithNoRollsLeft.builder()
            .friendlyUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .battleSite(SEA_ZONE)
            .hasRetreatTerritories(false)
            .isAttacker(false)
            .build()
            .find();

    assertThat(result, is(List.of(friendlyUnits.get(0))));
  }
}
