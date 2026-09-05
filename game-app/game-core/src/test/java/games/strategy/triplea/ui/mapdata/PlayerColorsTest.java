package games.strategy.triplea.ui.mapdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class PlayerColorsTest {

  @Nested
  class DefaultPlayerColors {
    @Test
    @DisplayName("If a player name is not in the properties, they get a (stable) default color")
    void missingPlayersWillGetDefaultColor() {
      final var playerColors = new PlayerColors(new Properties());

      final Color playerColor = playerColors.getPlayerColor("player");

      assertThat(playerColor).isNotNull();
    }

    @Test
    @DisplayName("Requesting a (default) player color multiple times should return the same color")
    void playerDefaultColorIsStable() {
      final var playerColors = new PlayerColors(new Properties());

      final Color playerColor = playerColors.getPlayerColor("player");
      final Color playerColorSecondTime = playerColors.getPlayerColor("player");

      assertThat(playerColorSecondTime)
          .as("Color should remain the same when requesting player color a second time")
          .isEqualTo(playerColor);
    }

    @Test
    @DisplayName(
        "Ensure that the a given player name will always get the same randomly "
            + "generated default color")
    void playerDefaultColorIsConsistent() {
      final PlayerColors playerColors0 = new PlayerColors(new Properties());
      final PlayerColors playerColors1 = new PlayerColors(new Properties());

      useUpAllDefaultColors(playerColors0);
      useUpAllDefaultColors(playerColors1);

      final Color playerColor0 = playerColors0.getPlayerColor("player");
      final Color playerColor1 = playerColors1.getPlayerColor("player");

      assertThat(playerColor0).isEqualTo(playerColor1);
    }

    private void useUpAllDefaultColors(final PlayerColors playerColors) {
      // use up all of the default colors such that the next default color will be random.
      for (int i = 0; i < PlayerColors.DEFAULT_COLOR_COUNT; i++) {
        playerColors.getPlayerColor("player" + i);
      }
    }

    @Test
    @DisplayName("Ensure randomly generated default colors are different")
    void randomlyGeneratedPlayerColorsWillBeDifferent() {
      final PlayerColors playerColors0 = new PlayerColors(new Properties());
      final PlayerColors playerColors1 = new PlayerColors(new Properties());

      useUpAllDefaultColors(playerColors0);
      useUpAllDefaultColors(playerColors1);

      final Color playerColor0 = playerColors0.getPlayerColor("player0");
      final Color playerColor1 = playerColors1.getPlayerColor("player1");

      assertThat(playerColor0)
          .as("player0 and player1 should have been assigned different, random, default colors")
          .isNotEqualTo(playerColor1);
    }

    @Test
    @DisplayName("Verify if we keep requesting default colors that we will continue getting colors")
    void unlimitedDefaultColors() {
      final var playerColors = new PlayerColors(new Properties());

      for (int i = 0; i < 100; i++) {
        assertThat(playerColors.getPlayerColor("player" + i)).isNotNull();
      }
    }
  }

  @Nested
  class GetPlayerColorWithPlayerColorDefinedInProperties {
    @Test
    void getPlayerColorSpecifiedInProperties() {
      final Properties properties = new Properties();
      properties.put(PlayerColors.PROPERTY_COLOR_PREFIX + "player", "00FF00");
      final var playerColors = new PlayerColors(properties);

      final Color playerColor = playerColors.getPlayerColor("player");

      assertThat(playerColor).isEqualTo(Color.GREEN);
    }
  }

  @Nested
  class GetImpassableColor {
    @Test
    void getPlayerColorThrowsIfPlayerNameIsImpassable() {
      final var playerColors = new PlayerColors(new Properties());

      assertThrows(
          IllegalArgumentException.class,
          () -> playerColors.getPlayerColor(PlayerColors.PLAYER_NAME_IMPASSABLE));
      assertThrows(
          IllegalArgumentException.class,
          () -> playerColors.getPlayerColor(PlayerColors.PLAYER_NAME_IMPASSABLE_LEGACY_SPELLING));
    }

    @Test
    void getImpassableColor() {
      final Properties properties = new Properties();
      properties.put(
          PlayerColors.PROPERTY_COLOR_PREFIX + PlayerColors.PLAYER_NAME_IMPASSABLE, "000000");
      final var playerColors = new PlayerColors(properties);

      final Color impassable = playerColors.getImpassableColor();
      assertThat(impassable).isEqualTo(Color.BLACK);
    }

    @Test
    void getImpassableColorAltSpelling() {
      final Properties properties = new Properties();
      properties.put(
          PlayerColors.PROPERTY_COLOR_PREFIX + PlayerColors.PLAYER_NAME_IMPASSABLE_LEGACY_SPELLING,
          "FFFFFF");
      final var playerColors = new PlayerColors(properties);

      final Color impassable = playerColors.getImpassableColor();

      assertThat(impassable).isEqualTo(Color.WHITE);
    }

    @Test
    @DisplayName("If the impassable color is not defined, ensure that we use a default for it")
    void defaultImpassableColorIfNotDefined() {
      final var playerColors = new PlayerColors(new Properties());

      final Color impassable = playerColors.getImpassableColor();

      assertThat(impassable).isEqualTo(PlayerColors.DEFAULT_IMPASSABLE_COLOR);
    }
  }
}
