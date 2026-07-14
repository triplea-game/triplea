package games.strategy.triplea.delegate.reinforcement;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.FixedReinforcementAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.List;
import org.junit.jupiter.api.Test;

class FixedReinforcementObservationFactoryTest {
  @Test
  void exposesQueuedAndScheduledDeliveriesInStableOrder() throws Exception {
    final GameData data = new GameData();
    final GamePlayer player = new GamePlayer("Allies", data);
    data.getPlayerList().addPlayerId(player);
    data.getMap().addTerritory(new Territory("Front", data));
    final UnitType infantry = new UnitType("infantry", data);
    infantry.addAttachment(
        Constants.UNIT_ATTACHMENT_NAME,
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, data));
    data.getUnitTypeList().addUnitType(infantry);
    final FixedReinforcementAttachment attachment =
        new FixedReinforcementAttachment("fixedReinforcementAttachment", player, data);
    attachment.setReinforcement("2:Front:infantry:2");
    player.addAttachment("fixedReinforcementAttachment", attachment);
    final FixedReinforcementTracker tracker = new FixedReinforcementTracker();
    tracker.completeRound(
        player, 1, List.of(new FixedReinforcementOrder(1, "Front", "infantry", 1)));

    final FixedReinforcementObservation observation =
        FixedReinforcementObservationFactory.create(data, player, tracker);

    assertThat(observation.schemaVersion()).isEqualTo(1);
    assertThat(observation.pending())
        .containsExactly(new FixedReinforcementObservation.Entry(1, "Front", "infantry", 1));
    assertThat(observation.scheduled())
        .containsExactly(new FixedReinforcementObservation.Entry(2, "Front", "infantry", 2));
  }
}
