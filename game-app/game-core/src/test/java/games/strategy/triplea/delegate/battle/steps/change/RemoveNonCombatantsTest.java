package games.strategy.triplea.delegate.battle.steps.change;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.battle.AirGroundBattlePolicy;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoveNonCombatantsTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock IDisplay display;
  @Mock BattleState battleState;
  @Mock BattleActions battleActions;
  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;

  @Test
  void notifiesBothOffenseAndDefenseNonCombat() {
    final RemoveNonCombatants removeNonCombatants =
        new RemoveNonCombatants(battleState, battleActions);

    when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(display);

    final Collection<Unit> offenseNonCombatants = List.of(mock(Unit.class));
    when(battleState.removeNonCombatants(BattleState.Side.OFFENSE))
        .thenReturn(offenseNonCombatants);
    final Collection<Unit> defenseNonCombatants = List.of(mock(Unit.class));
    when(battleState.removeNonCombatants(BattleState.Side.DEFENSE))
        .thenReturn(defenseNonCombatants);

    when(battleState.getPlayer(BattleState.Side.OFFENSE)).thenReturn(attacker);
    when(battleState.getPlayer(BattleState.Side.DEFENSE)).thenReturn(defender);

    removeNonCombatants.execute(executionStack, delegateBridge);

    verify(display, times(2).description("Both offense and defense should be notified"))
        .changedUnitsNotification(any(), any(), any(), any(), any());
    verify(display)
        .changedUnitsNotification(
            any(), eq(attacker), eq(offenseNonCombatants), eq(null), eq(null));
    verify(display)
        .changedUnitsNotification(
            any(), eq(defender), eq(defenseNonCombatants), eq(null), eq(null));
  }

  @Test
  void doesNotNotifyDefenseIfNoDefenseNonCombatants() {
    final RemoveNonCombatants removeNonCombatants =
        new RemoveNonCombatants(battleState, battleActions);

    when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(display);

    final Collection<Unit> offenseNonCombatants = List.of(mock(Unit.class));
    when(battleState.removeNonCombatants(BattleState.Side.OFFENSE))
        .thenReturn(offenseNonCombatants);
    final Collection<Unit> defenseNonCombatants = List.of();
    when(battleState.removeNonCombatants(BattleState.Side.DEFENSE))
        .thenReturn(defenseNonCombatants);

    when(battleState.getPlayer(BattleState.Side.OFFENSE)).thenReturn(attacker);

    removeNonCombatants.execute(executionStack, delegateBridge);

    verify(display, times(1).description("Only offense should be notified"))
        .changedUnitsNotification(any(), any(), any(), any(), any());
    verify(display)
        .changedUnitsNotification(
            any(), eq(attacker), eq(offenseNonCombatants), eq(null), eq(null));
  }

  @Test
  void doesNotNotifyOffenseIfNoOffenseNonCombatants() {
    final RemoveNonCombatants removeNonCombatants =
        new RemoveNonCombatants(battleState, battleActions);

    when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(display);

    final Collection<Unit> offenseNonCombatants = List.of();
    when(battleState.removeNonCombatants(BattleState.Side.OFFENSE))
        .thenReturn(offenseNonCombatants);
    final Collection<Unit> defenseNonCombatants = List.of(mock(Unit.class));
    when(battleState.removeNonCombatants(BattleState.Side.DEFENSE))
        .thenReturn(defenseNonCombatants);

    when(battleState.getPlayer(BattleState.Side.DEFENSE)).thenReturn(defender);

    removeNonCombatants.execute(executionStack, delegateBridge);

    verify(display, times(1).description("Only defense should be notified"))
        .changedUnitsNotification(any(), any(), any(), any(), any());
    verify(display)
        .changedUnitsNotification(
            any(), eq(defender), eq(defenseNonCombatants), eq(null), eq(null));
  }

  @Test
  void marksAircraftAsHandledByAirDomainBeforeGroundCombat() {
    final RemoveNonCombatants removeNonCombatants =
        new RemoveNonCombatants(battleState, battleActions);
    final GameData gameData = mock(GameData.class);
    final GameProperties gameProperties = mock(GameProperties.class);
    final PlayerList playerList = mock(PlayerList.class);
    final UnitType unitType = new UnitType("aircraft", gameData);
    final UnitAttachment unitAttachment = mock(UnitAttachment.class);
    unitType.addAttachment(Constants.UNIT_ATTACHMENT_NAME, unitAttachment);

    when(gameData.getPlayerList()).thenReturn(playerList);
    when(playerList.getNullPlayer()).thenReturn(attacker);
    final Unit aircraft = new Unit(unitType, attacker, gameData);

    when(delegateBridge.getData()).thenReturn(gameData);
    when(gameData.getProperties()).thenReturn(gameProperties);
    when(gameProperties.get(AirGroundBattlePolicy.SEPARATE_AIR_AND_GROUND_COMBAT, false))
        .thenReturn(true);
    when(unitAttachment.isAir()).thenReturn(true);
    when(battleState.filterUnits(
            BattleState.UnitBattleFilter.ACTIVE,
            BattleState.Side.OFFENSE,
            BattleState.Side.DEFENSE))
        .thenReturn(List.of(aircraft));
    when(battleState.removeNonCombatants(BattleState.Side.OFFENSE)).thenReturn(List.of());
    when(battleState.removeNonCombatants(BattleState.Side.DEFENSE)).thenReturn(List.of());

    removeNonCombatants.execute(executionStack, delegateBridge);

    verify(delegateBridge).addChange(any(Change.class));
  }

  @Test
  void survivingOffenseAircraftEstablishAirControl() throws Exception {
    final RemoveNonCombatants removeNonCombatants =
        new RemoveNonCombatants(battleState, battleActions);
    final GameData gameData = new GameData();
    final GamePlayer offense = new GamePlayer("Blue", gameData);
    final GamePlayer defense = new GamePlayer("Red", gameData);
    gameData.getPlayerList().addPlayerId(offense);
    gameData.getPlayerList().addPlayerId(defense);
    final Territory territory = new Territory("Front", gameData);
    gameData.getMap().addTerritory(territory);
    gameData.getProperties().set(AirGroundBattlePolicy.SEPARATE_AIR_AND_GROUND_COMBAT, true);
    gameData.getProperties().set(AirControlTracker.AIR_CONTROL_ENABLED, true);
    final UnitType aircraftType = new UnitType("fighter", gameData);
    final UnitAttachment aircraftAttachment =
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, aircraftType, gameData);
    aircraftType.addAttachment(Constants.UNIT_ATTACHMENT_NAME, aircraftAttachment);
    aircraftAttachment.getPropertyOrEmpty("isAir").orElseThrow().setValue(true);
    final Unit aircraft = new Unit(aircraftType, offense, gameData);
    final IDelegateHistoryWriter historyWriter = mock(IDelegateHistoryWriter.class);

    when(delegateBridge.getData()).thenReturn(gameData);
    when(delegateBridge.getHistoryWriter()).thenReturn(historyWriter);
    when(battleState.getBattleSite()).thenReturn(territory);
    when(battleState.getPlayer(BattleState.Side.OFFENSE)).thenReturn(offense);
    when(battleState.filterUnits(BattleState.UnitBattleFilter.ACTIVE, BattleState.Side.OFFENSE))
        .thenReturn(List.of(aircraft));
    when(battleState.filterUnits(BattleState.UnitBattleFilter.ACTIVE, BattleState.Side.DEFENSE))
        .thenReturn(List.of());
    when(battleState.filterUnits(
            BattleState.UnitBattleFilter.ACTIVE,
            BattleState.Side.OFFENSE,
            BattleState.Side.DEFENSE))
        .thenReturn(List.of(aircraft));
    when(battleState.removeNonCombatants(BattleState.Side.OFFENSE)).thenReturn(List.of());
    when(battleState.removeNonCombatants(BattleState.Side.DEFENSE)).thenReturn(List.of());

    removeNonCombatants.execute(executionStack, delegateBridge);

    final ArgumentCaptor<Change> changes = ArgumentCaptor.forClass(Change.class);
    verify(delegateBridge, times(2)).addChange(changes.capture());
    gameData.performChange(changes.getAllValues().get(0));
    assertThat(AirControlTracker.get(gameData).getController(territory, gameData))
        .contains(offense);
    verify(historyWriter).addChildToEvent("Blue gains air control over Front");
  }
}
