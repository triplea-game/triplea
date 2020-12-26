package games.strategy.triplea.delegate.battle.steps.fire.battlegroup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.junit.jupiter.api.Test;

class BombardmentBattleGroupsTest {

  @Test
  void onlyOneOffenseSquadron() {
    final GameProperties properties = new GameProperties(mock(GameData.class));
    final BattleGroup battleGroup = BombardmentBattleGroups.create(properties);

    assertThat(battleGroup.getOffenseSquadrons(), hasSize(1));
  }

  @Test
  void noDefenseSquadrons() {
    final GameProperties properties = new GameProperties(mock(GameData.class));
    final BattleGroup battleGroup = BombardmentBattleGroups.create(properties);

    assertThat(battleGroup.getDefenseSquadrons(), is(empty()));
  }

  @Test
  void offenseSquadronIsIgnoredOnRound2() {
    final GameProperties properties = new GameProperties(mock(GameData.class));
    final BattleGroup battleGroup = BombardmentBattleGroups.create(properties);
    final FiringSquadron firingSquadron = battleGroup.getOffenseSquadrons().iterator().next();
    final BattleState battleState = mock(BattleState.class);
    final BattleState.BattleStatus battleStatus =
        BattleState.BattleStatus.of(2, 10, false, true, false);
    when(battleState.getStatus()).thenReturn(battleStatus);

    assertThat(firingSquadron.getBattleStateRequirements().test(battleState), is(false));
  }

  @Test
  void offenseSquadronIsIgnoredIfWaterTerritory() {
    final GameProperties properties = new GameProperties(mock(GameData.class));
    final BattleGroup battleGroup = BombardmentBattleGroups.create(properties);
    final FiringSquadron firingSquadron = battleGroup.getOffenseSquadrons().iterator().next();
    final BattleState battleState = mock(BattleState.class);
    final BattleState.BattleStatus battleStatus =
        BattleState.BattleStatus.of(1, 10, false, true, false);
    when(battleState.getStatus()).thenReturn(battleStatus);
    final Territory battleSite = mock(Territory.class);
    when(battleSite.isWater()).thenReturn(true);
    when(battleState.getBattleSite()).thenReturn(battleSite);

    assertThat(firingSquadron.getBattleStateRequirements().test(battleState), is(false));
  }

  @Test
  void offenseSquadronIsIgnoredIfNoBombardingUnits() {
    final GameProperties properties = new GameProperties(mock(GameData.class));
    final BattleGroup battleGroup = BombardmentBattleGroups.create(properties);
    final FiringSquadron firingSquadron = battleGroup.getOffenseSquadrons().iterator().next();
    final BattleState battleState = mock(BattleState.class);
    final BattleState.BattleStatus battleStatus =
        BattleState.BattleStatus.of(1, 10, false, true, false);
    when(battleState.getStatus()).thenReturn(battleStatus);
    final Territory battleSite = mock(Territory.class);
    when(battleSite.isWater()).thenReturn(false);
    when(battleState.getBattleSite()).thenReturn(battleSite);
    when(battleState.getBombardingUnits()).thenReturn(List.of());

    assertThat(firingSquadron.getBattleStateRequirements().test(battleState), is(false));
  }
}
