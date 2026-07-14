package games.strategy.triplea.delegate.supply;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.List;
import org.junit.jupiter.api.Test;

class SupplyAwareMoveDelegateTest {
  @Test
  void rejectsLandUnitBeforeLegacyMovementValidationWhenSupplyIsCut() {
    final GameData data = new GameData();
    final GamePlayer player = new GamePlayer("Blue", data);
    data.getPlayerList().addPlayerId(player);
    final Territory start = new Territory("Start", data);
    final Territory end = new Territory("End", data);
    start.setOwner(player);
    end.setOwner(player);
    data.getMap().addTerritory(start);
    data.getMap().addTerritory(end);
    final UnitType infantry = new UnitType("infantry", data);
    infantry.addAttachment(
        Constants.UNIT_ATTACHMENT_NAME,
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, data));
    data.getUnitTypeList().addUnitType(infantry);
    final Unit unit = infantry.create(1, player).getFirst();
    start.getUnitCollection().add(unit);
    data.getProperties().set(SupplyNetworkResolver.SUPPLY_NETWORK_ENABLED, true);

    final IDelegateBridge bridge = mock(IDelegateBridge.class);
    when(bridge.getData()).thenReturn(data);
    when(bridge.getGamePlayer()).thenReturn(player);
    final SupplyAwareMoveDelegate delegate = new SupplyAwareMoveDelegate();
    delegate.setDelegateBridgeAndPlayer(bridge);

    final var result =
        delegate.performMove(new MoveDescription(List.of(unit), new Route(start, end)));

    assertThat(result)
        .contains(SupplyAwareMoveDelegate.OUT_OF_SUPPLY_UNITS_CANNOT_MOVE + ": 1 infantry");
  }
}
