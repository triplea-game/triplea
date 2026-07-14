package games.strategy.triplea.delegate.reinforcement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.FixedReinforcementAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FixedReinforcementServiceTest {
  private final GameData data = new GameData();
  private final GamePlayer player = new GamePlayer("Allies", data);
  private final Territory front = new Territory("Front", data);
  private final UnitType infantry = new UnitType("infantry", data);
  private final FixedReinforcementAttachment attachment =
      new FixedReinforcementAttachment("fixedReinforcementAttachment", player, data);
  private final FixedReinforcementTracker tracker = new FixedReinforcementTracker();
  private final IDelegateBridge bridge = mock(IDelegateBridge.class);

  @BeforeEach
  void setUp() {
    data.getPlayerList().addPlayerId(player);
    data.getMap().addTerritory(front);
    front.setOwner(player);
    TerritoryAttachment.add(
        front, new TerritoryAttachment(Constants.TERRITORY_ATTACHMENT_NAME, front, data));
    infantry.addAttachment(
        Constants.UNIT_ATTACHMENT_NAME,
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, data));
    data.getUnitTypeList().addUnitType(infantry);
    player.addAttachment("fixedReinforcementAttachment", attachment);
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
  void placesDueUnitsAndIsIdempotentWithinTheRound() throws Exception {
    attachment.setReinforcement("1:Front:infantry:2");

    FixedReinforcementService.apply(bridge, player, tracker);
    FixedReinforcementService.apply(bridge, player, tracker);

    assertThat(front.getUnitCollection().getUnitCount()).isEqualTo(2);
    assertThat(tracker.getPending(player)).isEmpty();
    assertThat(tracker.getLastProcessedRound(player)).isEqualTo(1);
  }

  @Test
  void carriesBlockedOrdersToTheNextOwnerRound() throws Exception {
    attachment.setReinforcement("1:Front:infantry:3");
    front.setOwner(data.getPlayerList().getNullPlayer());

    FixedReinforcementService.apply(bridge, player, tracker);
    assertThat(front.getUnitCollection().getUnitCount()).isZero();
    assertThat(tracker.getPending(player))
        .containsExactly(new FixedReinforcementOrder(1, "Front", "infantry", 3));

    front.setOwner(player);
    data.getSequence().setRoundOffset(1);
    FixedReinforcementService.apply(bridge, player, tracker);

    assertThat(front.getUnitCollection().getUnitCount()).isEqualTo(3);
    assertThat(tracker.getPending(player)).isEmpty();
  }
}
