package games.strategy.engine.framework.startup.ui;

import static java.util.Arrays.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import games.strategy.engine.player.Player;
import games.strategy.triplea.settings.ClientSetting;
import java.util.List;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;

class PlayerTypeTest {

  @BeforeEach
  @SuppressWarnings("static-method")
  void initializeClientSettingPreferences() {
    ClientSetting.setPreferences(new MemoryPreferences());
  }

  @Test
  void playerTypes() {
    assertThat(
        "Ensure we do not have an example invisible player type in the selection list",
        List.of(PlayerType.playerTypes()),
        Matchers.not(IsCollectionContaining.hasItem(PlayerType.CLIENT_PLAYER.getLabel())));

    assertThat(
        "Ensure we have a visible player type in the selection list",
        List.of(PlayerType.playerTypes()),
        IsCollectionContaining.hasItem(PlayerType.HUMAN_PLAYER.getLabel()));
  }

  @Test
  void newPlayerWithName() {
    final String testName = "example";

    stream(PlayerType.values())
        .filter(playerType -> playerType != PlayerType.BATTLE_CALC_DUMMY)
        .forEach(
            playerType -> {
              final Player result = playerType.newPlayerWithName(testName);
              assertThat(
                  "The player type should match after construction, input type: " + playerType,
                  result.getPlayerType(),
                  is(playerType));
              assertThat(
                  "The name is a passed in parameter, this should still match after construction",
                  result.getName(),
                  is(testName));
            });
  }

  @Test
  void fromLabel() {
    assertThrows(IllegalStateException.class, () -> PlayerType.fromLabel("invalid_label_type"));

    stream(PlayerType.values())
        .forEach(
            playerType ->
                assertThat(
                    "Make sure that we can reconstruct each player type from its label",
                    PlayerType.fromLabel(playerType.getLabel()),
                    is(playerType)));
  }

  @Test
  void getLabel() {
    assertThat(
        "All player type labels should be unique, count of unique labels should match total",
        stream(PlayerType.values()).map(PlayerType::getLabel).distinct().count(),
        is((long) PlayerType.values().length));

    assertThat(
        "No label should be empty ",
        stream(PlayerType.values()).map(PlayerType::getLabel).anyMatch(String::isEmpty),
        is(false));
  }
}
