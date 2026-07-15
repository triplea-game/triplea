package games.strategy.triplea.delegate.battle.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BattleObservationAirControlTest {
  @Test
  void exposesControllerAndOffenseGroundBonus() {
    final GameData gameData = new GameData();
    final GamePlayer offense = new GamePlayer("Blue", gameData);
    final GamePlayer defense = new GamePlayer("Red", gameData);
    gameData.getPlayerList().addPlayerId(offense);
    gameData.getPlayerList().addPlayerId(defense);
    final Territory territory = new Territory("Front", gameData);
    gameData.getMap().addTerritory(territory);
    gameData.getProperties().set(AirControlTracker.AIR_CONTROL_ENABLED, true);
    gameData.performChange(AirControlTracker.changeControl(territory, offense, gameData));
    final BattleState battleState = mock(BattleState.class);

    when(battleState.getGameData()).thenReturn(gameData);
    when(battleState.getBattleId()).thenReturn(UUID.randomUUID());
    when(battleState.getBattleSite()).thenReturn(territory);
    when(battleState.getStatus()).thenReturn(BattleState.BattleStatus.of(1, 2, false, false, true));
    when(battleState.getPlayer(BattleState.Side.OFFENSE)).thenReturn(offense);
    when(battleState.getPlayer(BattleState.Side.DEFENSE)).thenReturn(defense);
    when(battleState.filterUnits(BattleState.UnitBattleFilter.ALIVE, BattleState.Side.OFFENSE))
        .thenReturn(List.of());
    when(battleState.filterUnits(BattleState.UnitBattleFilter.ALIVE, BattleState.Side.DEFENSE))
        .thenReturn(List.of());
    when(battleState.getAttackerRetreatTerritories()).thenReturn(List.of());

    final BattleObservation observation = BattleObservationFactory.create(battleState);

    assertThat(observation.schemaVersion()).isEqualTo(4);
    assertThat(observation.airControlPlayer()).isEqualTo("Blue");
    assertThat(observation.offenseGroundAttackBonus()).isEqualTo(1);
  }
}
