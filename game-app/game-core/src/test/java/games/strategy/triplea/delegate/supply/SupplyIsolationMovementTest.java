package games.strategy.triplea.delegate.supply;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import org.junit.jupiter.api.Test;

class SupplyIsolationMovementTest {
  @Test
  void roadCutBlocksMovementOnlyAfterOwnerSupplyEvaluation() {
    final GameData data = new GameData();
    final GamePlayer player = new GamePlayer("Blue", data);
    data.getPlayerList().addPlayerId(player);
    final Territory front = new Territory("Front", data);
    front.setOwner(player);
    data.getMap().addTerritory(front);
    final UnitType infantry = new UnitType("infantry", data);
    infantry.addAttachment(
        Constants.UNIT_ATTACHMENT_NAME,
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, data));
    data.getUnitTypeList().addUnitType(infantry);
    final Unit unit = infantry.create(1, player).getFirst();
    front.getUnitCollection().add(unit);
    data.getProperties().set(SupplyNetworkResolver.SUPPLY_NETWORK_ENABLED, true);
    final SupplyTracker tracker = new SupplyTracker();

    assertThat(SupplyNetworkResolver.isSupplied(front, player, data)).isFalse();
    assertThat(SupplyNetworkResolver.canMove(unit, front, player, data, tracker)).isTrue();

    tracker.increment(unit);

    assertThat(SupplyNetworkResolver.canMove(unit, front, player, data, tracker)).isFalse();
    assertThat(tracker.getOutOfSupplyTurns(unit)).isEqualTo(1);
    assertThat(SupplyNetworkResolver.getRemovalTurns(data) - tracker.getOutOfSupplyTurns(unit))
        .isEqualTo(1);
  }
}
