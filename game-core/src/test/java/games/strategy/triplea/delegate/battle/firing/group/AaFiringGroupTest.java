package games.strategy.triplea.delegate.battle.firing.group;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AaFiringGroupTest {

  private static final GameData FIRING_GROUPS_GAME_DATA =
      TestMapGameData.FIRING_GROUPS.getGameData();
  private static final GamePlayer BRITAIN = GameDataTestUtil.britain(FIRING_GROUPS_GAME_DATA);
  private static final GamePlayer GERMANY = GameDataTestUtil.germany(FIRING_GROUPS_GAME_DATA);

  @Test
  @DisplayName("Verify AA firing group has all AA units and their targets.")
  void getFiringGroups() {
    final Collection<Unit> attackingUnits =
        GameDataTestUtil.antiTankGun(FIRING_GROUPS_GAME_DATA).create(1, GERMANY);
    final Collection<Unit> defendingUnits =
        GameDataTestUtil.tank(FIRING_GROUPS_GAME_DATA).create(1, BRITAIN);

    final List<FiringGroup> result =
        AaFiringGroup.builder()
            .firingUnits(attackingUnits)
            .attackableUnits(defendingUnits)
            .defending(false)
            .gameData(FIRING_GROUPS_GAME_DATA)
            .hitPlayer(BRITAIN)
            .build()
            .getFiringGroups();

    final FiringGroup expected =
        FiringGroup.of(attackingUnits, defendingUnits, false, "AntiTankGun");

    assertThat(result, is(List.of(expected)));
  }

  @Test
  @DisplayName("Verify 2 AA firing groups for different AaTypes")
  void getDifferentAaTypeFiringGroups() {
    final List<Unit> attackingUnits =
        GameDataTestUtil.antiTankGun(FIRING_GROUPS_GAME_DATA).create(1, GERMANY);
    attackingUnits.addAll(GameDataTestUtil.antiAirGun(FIRING_GROUPS_GAME_DATA).create(1, GERMANY));
    final List<Unit> defendingUnits =
        GameDataTestUtil.tank(FIRING_GROUPS_GAME_DATA).create(1, BRITAIN);
    defendingUnits.addAll(GameDataTestUtil.fighter(FIRING_GROUPS_GAME_DATA).create(1, BRITAIN));

    final List<FiringGroup> result =
        AaFiringGroup.builder()
            .firingUnits(attackingUnits)
            .attackableUnits(defendingUnits)
            .defending(false)
            .gameData(FIRING_GROUPS_GAME_DATA)
            .hitPlayer(BRITAIN)
            .build()
            .getFiringGroups();

    final List<FiringGroup> expected =
        List.of(
            FiringGroup.of(
                attackingUnits.subList(0, 1), List.of(defendingUnits.get(0)), false, "AntiTankGun"),
            FiringGroup.of(
                attackingUnits.subList(1, 2), List.of(defendingUnits.get(1)), false, "AntiAirGun"));

    assertThat(result, is(expected));
  }

  @Test
  @DisplayName("Verify one aa type with a suicide unit and non suicide unit make 2 firing groups")
  void getAaTypeWithSuicideAndNonSuicide() {
    final List<Unit> attackingUnits =
        GameDataTestUtil.antiAirGun(FIRING_GROUPS_GAME_DATA).create(1, GERMANY);
    attackingUnits.addAll(
        GameDataTestUtil.suicideAntiAirGun(FIRING_GROUPS_GAME_DATA).create(1, GERMANY));
    final List<Unit> defendingUnits =
        GameDataTestUtil.fighter(FIRING_GROUPS_GAME_DATA).create(1, BRITAIN);

    final List<FiringGroup> result =
        AaFiringGroup.builder()
            .firingUnits(attackingUnits)
            .attackableUnits(defendingUnits)
            .defending(false)
            .gameData(FIRING_GROUPS_GAME_DATA)
            .hitPlayer(BRITAIN)
            .build()
            .getFiringGroups();

    final List<FiringGroup> expected =
        List.of(
            FiringGroup.of(attackingUnits.subList(1, 2), defendingUnits, true, "AntiAirGun"),
            FiringGroup.of(attackingUnits.subList(0, 1), defendingUnits, false, "AntiAirGun"));

    assertThat(result, is(expected));
  }
}
