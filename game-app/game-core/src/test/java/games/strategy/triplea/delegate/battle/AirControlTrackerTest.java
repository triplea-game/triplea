package games.strategy.triplea.delegate.battle;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AirControlTrackerTest {
  private GameData gameData;
  private GamePlayer player;
  private Territory territory;

  @BeforeEach
  void setUp() {
    gameData = new GameData();
    player = new GamePlayer("Blue", gameData);
    gameData.getPlayerList().addPlayerId(player);
    territory = new Territory("Front", gameData);
    gameData.getMap().addTerritory(territory);
    gameData.getProperties().set(AirControlTracker.AIR_CONTROL_ENABLED, true);
  }

  @Test
  void changeIsInvertibleAndIndependentOfGroundOwnership() {
    final GamePlayer groundOwner = territory.getOwner();
    final Change change = AirControlTracker.changeControl(territory, player, gameData);

    gameData.performChange(change);

    assertThat(AirControlTracker.get(gameData).getController(territory, gameData)).contains(player);
    assertThat(AirControlTracker.get(gameData).getStatus(territory, gameData))
        .isEqualTo(AirControlTracker.Status.CONTROLLED);
    assertThat(territory.getOwner()).isSameAs(groundOwner);

    gameData.performChange(change.invert());

    assertThat(AirControlTracker.get(gameData).getController(territory, gameData)).isEmpty();
    assertThat(AirControlTracker.get(gameData).getStatus(territory, gameData))
        .isEqualTo(AirControlTracker.Status.UNCONTROLLED);
    assertThat(territory.getOwner()).isSameAs(groundOwner);
  }

  @Test
  void contestedStateIsDistinctFromUncontrolledAndInvertible() {
    final Change controlled = AirControlTracker.changeControl(territory, player, gameData);
    gameData.performChange(controlled);
    final Change contested = AirControlTracker.changeContested(territory, gameData);

    gameData.performChange(contested);

    assertThat(AirControlTracker.get(gameData).getStatus(territory, gameData))
        .isEqualTo(AirControlTracker.Status.CONTESTED);
    assertThat(AirControlTracker.get(gameData).getController(territory, gameData)).isEmpty();
    assertThat(AirControlTracker.get(gameData).snapshot(gameData))
        .containsEntry("Front", "CONTESTED");

    gameData.performChange(contested.invert());

    assertThat(AirControlTracker.get(gameData).getStatus(territory, gameData))
        .isEqualTo(AirControlTracker.Status.CONTROLLED);
    assertThat(AirControlTracker.get(gameData).getController(territory, gameData)).contains(player);
  }

  @Test
  void turnLimitedControlExpiresAfterTheRound() {
    gameData.performChange(AirControlTracker.changeControl(territory, player, gameData));

    gameData.getSequence().setRoundOffset(1);

    assertThat(AirControlTracker.get(gameData).getController(territory, gameData)).isEmpty();
  }

  @Test
  void persistentControlSurvivesLaterRounds() {
    gameData.getProperties().set(AirControlTracker.AIR_CONTROL_PERSISTENT, true);
    gameData.performChange(AirControlTracker.changeControl(territory, player, gameData));

    gameData.getSequence().setRoundOffset(3);

    assertThat(AirControlTracker.get(gameData).getController(territory, gameData)).contains(player);
  }

  @Test
  void alliedLandAttackBonusUsesConfiguredValueAndNeverAppliesAtSea() {
    gameData.getProperties().set(AirControlTracker.AIR_CONTROL_GROUND_ATTACK_BONUS, 2);
    gameData.performChange(AirControlTracker.changeControl(territory, player, gameData));
    final Territory sea = new Territory("Sea", true, gameData);
    gameData.getMap().addTerritory(sea);
    gameData.performChange(AirControlTracker.changeControl(sea, player, gameData));

    assertThat(AirControlTracker.get(gameData).getGroundAttackBonus(territory, player, gameData))
        .isEqualTo(2);
    assertThat(AirControlTracker.get(gameData).getGroundAttackBonus(sea, player, gameData))
        .isZero();
  }

  @Test
  void trackerStateIsJavaSerializable() throws Exception {
    gameData.performChange(AirControlTracker.changeControl(territory, player, gameData));
    final AirControlTracker serialized = AirControlTracker.get(gameData);
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
      output.writeObject(serialized);
    }

    final AirControlTracker restored;
    try (ObjectInputStream input =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      restored = (AirControlTracker) input.readObject();
    }
    gameData.getProperties().set(AirControlTracker.STATE_PROPERTY, restored);

    assertThat(AirControlTracker.get(gameData).getController(territory, gameData)).contains(player);
  }
}
