package games.strategy.triplea.attachments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import nl.jqno.equalsverifier.EqualsVerifier;

final class TerritoryAttachmentTest {
  @Nested
  final class GetCaptureOwnershipChangesTest {
    private final GameData gameData = new GameData();
    private final PlayerID player1 = new PlayerID("player1", gameData);
    private final PlayerID player2 = new PlayerID("player2", gameData);
    private final PlayerID player3 = new PlayerID("player3", gameData);
    private final TerritoryAttachment territoryAttachment =
        new TerritoryAttachment("territoryAttachment", null, gameData);

    private String join(final String... values) {
      return String.join(":", values);
    }

    @BeforeEach
    void setUpGameData() {
      gameData.getPlayerList().addPlayerId(player1);
      gameData.getPlayerList().addPlayerId(player2);
      gameData.getPlayerList().addPlayerId(player3);
    }

    @Test
    void shouldReturnEmptyListWhenZeroCaptureOwnershipChangesExist() {
      assertThat(territoryAttachment.getCaptureOwnershipChanges(), hasSize(0));
    }

    @Test
    void shouldReturnListOfSizeOneWhenOneCaptureOwnershipChangeExists() throws Exception {
      territoryAttachment.setWhenCapturedByGoesTo(join(player1.getName(), player2.getName()));

      assertThat(
          territoryAttachment.getCaptureOwnershipChanges(),
          contains(new TerritoryAttachment.CaptureOwnershipChange(player1, player2)));
    }

    @Test
    void shouldReturnListOfSizeTwoWhenTwoCaptureOwnershipChangesExists() throws Exception {
      territoryAttachment.setWhenCapturedByGoesTo(join(player1.getName(), player3.getName()));
      territoryAttachment.setWhenCapturedByGoesTo(join(player2.getName(), player3.getName()));

      assertThat(
          territoryAttachment.getCaptureOwnershipChanges(),
          contains(
              new TerritoryAttachment.CaptureOwnershipChange(player1, player3),
              new TerritoryAttachment.CaptureOwnershipChange(player2, player3)));
    }
  }

  @Nested
  final class CaptureOwnershipChangeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      final GameData gameData = new GameData();
      EqualsVerifier.forClass(TerritoryAttachment.CaptureOwnershipChange.class)
          .withPrefabValues(
              PlayerID.class,
              new PlayerID("redPlayerId", gameData),
              new PlayerID("blackPlayerId", gameData))
          .verify();
    }
  }
}
