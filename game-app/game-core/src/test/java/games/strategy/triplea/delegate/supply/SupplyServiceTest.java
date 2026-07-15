package games.strategy.triplea.delegate.supply;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupplyServiceTest {
  private final GameData data = new GameData();
  private final GamePlayer player = new GamePlayer("Blue", data);
  private final Territory front = new Territory("Front", data);
  private final UnitType infantry = new UnitType("infantry", data);
  private final SupplyTerritoryAttachment supplyAttachment =
      new SupplyTerritoryAttachment("supplyAttachment", front, data);
  private final SupplyTracker tracker = new SupplyTracker();
  private final IDelegateBridge bridge = mock(IDelegateBridge.class);
  private Unit unit;

  @BeforeEach
  void setUp() {
    data.getPlayerList().addPlayerId(player);
    data.getMap().addTerritory(front);
    front.setOwner(player);
    front.addAttachment("supplyAttachment", supplyAttachment);
    infantry.addAttachment(
        Constants.UNIT_ATTACHMENT_NAME,
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, data));
    data.getUnitTypeList().addUnitType(infantry);
    unit = infantry.create(1, player).getFirst();
    front.getUnitCollection().add(unit);
    data.getProperties().set(SupplyNetworkResolver.SUPPLY_NETWORK_ENABLED, true);
    when(bridge.getData()).thenReturn(data);
    when(bridge.getHistoryWriter()).thenReturn(mock(IDelegateHistoryWriter.class));
    doAnswer(
            invocation -> {
              data.performChange(invocation.getArgument(0, Change.class));
              return null;
            })
        .when(bridge)
        .addChange(any(Change.class));
  }

  @Test
  void removesUnitAfterTwoDistinctOwnerTurnsWithoutSupply() {
    SupplyService.apply(bridge, player, tracker);
    SupplyService.apply(bridge, player, tracker);

    assertThat(front.getUnitCollection()).contains(unit);
    assertThat(tracker.getOutOfSupplyTurns(unit)).isEqualTo(1);

    data.getSequence().setRoundOffset(1);
    SupplyService.apply(bridge, player, tracker);

    assertThat(front.getUnitCollection()).doesNotContain(unit);
    assertThat(tracker.getOutOfSupplyTurns(unit)).isZero();
    assertThat(tracker.getLastProcessedRound(player)).isEqualTo(2);
  }

  @Test
  void restoredSupplyClearsCounterWithoutRemovingUnit() throws Exception {
    SupplyService.apply(bridge, player, tracker);
    supplyAttachment.setSupplySource("true");
    data.getSequence().setRoundOffset(1);

    SupplyService.apply(bridge, player, tracker);

    assertThat(front.getUnitCollection()).contains(unit);
    assertThat(tracker.getOutOfSupplyTurns(unit)).isZero();
  }
}
