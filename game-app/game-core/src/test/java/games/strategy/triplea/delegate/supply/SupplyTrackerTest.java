package games.strategy.triplea.delegate.supply;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupplyTrackerTest {
  private final GameData data = new GameData();
  private final GamePlayer blue = new GamePlayer("Blue", data);
  private final GamePlayer red = new GamePlayer("Red", data);
  private Unit unit;

  @BeforeEach
  void setUp() {
    data.getPlayerList().addPlayerId(blue);
    data.getPlayerList().addPlayerId(red);
    final UnitType infantry = new UnitType("infantry", data);
    infantry.addAttachment(
        Constants.UNIT_ATTACHMENT_NAME,
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, data));
    data.getUnitTypeList().addUnitType(infantry);
    unit = new Unit(infantry, blue, data);
  }

  @Test
  void incrementsOncePerProcessedOwnerTurnAndResetsOnOwnerChange() {
    final SupplyTracker tracker = new SupplyTracker();

    assertThat(tracker.increment(unit)).isEqualTo(1);
    assertThat(tracker.increment(unit)).isEqualTo(2);
    tracker.completeRound(blue, 1);

    assertThat(tracker.shouldProcess(blue, 1)).isFalse();
    assertThat(tracker.shouldProcess(blue, 2)).isTrue();
    unit.setOwner(red);
    assertThat(tracker.increment(unit)).isEqualTo(1);
  }

  @Test
  void survivesJavaSerialization() throws Exception {
    final SupplyTracker tracker = new SupplyTracker();
    tracker.increment(unit);
    tracker.completeRound(blue, 1);
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
      output.writeObject(tracker);
    }

    final SupplyTracker restored;
    try (ObjectInputStream input =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      restored = (SupplyTracker) input.readObject();
    }

    assertThat(restored.getOutOfSupplyTurns(unit)).isEqualTo(1);
    assertThat(restored.getLastProcessedRound(blue)).isEqualTo(1);
  }
}
