package games.strategy.engine.framework.startup.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import games.strategy.engine.player.Player;
import java.util.List;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.jupiter.api.Test;

class PlayerTypesTest {

  @Test
  void playerTypes() {
    final PlayerTypes playerTypes = new PlayerTypes(PlayerTypes.getBuiltInPlayerTypes());
    assertThat(
        "Ensure we have a visible player type in the selection list",
        List.of(playerTypes.getAvailablePlayerLabels()),
        IsCollectionContaining.hasItem(PlayerTypes.WEAK_AI.getLabel()));
  }

  @Test
  void newPlayerWithName() {
    final String testName = "example";

    final PlayerTypes playerTypesProvider = new PlayerTypes(PlayerTypes.getBuiltInPlayerTypes());
    playerTypesProvider
        .getPlayerTypes()
        .forEach(
            playerType -> {
              final Player result = playerType.newPlayerWithName(testName);
              assertThat(
                  "The player label should match after construction, input type: " + playerType,
                  result.getPlayerLabel(),
                  is(playerType.getLabel()));
              assertThat(
                  "The name is a passed in parameter, this should still match after construction",
                  result.getName(),
                  is(testName));
            });
  }

  @Test
  void fromLabel() {
    final PlayerTypes playerTypesProvider = new PlayerTypes(PlayerTypes.getBuiltInPlayerTypes());
    assertThrows(
        IllegalStateException.class, () -> playerTypesProvider.fromLabel("invalid_label_type"));

    playerTypesProvider
        .getPlayerTypes()
        .forEach(
            playerType ->
                assertThat(
                    "Make sure that we can reconstruct each player type from its label",
                    playerTypesProvider.fromLabel(playerType.getLabel()),
                    is(playerType)));
  }

  @Test
  void getLabel() {
    final PlayerTypes playerTypes = new PlayerTypes(PlayerTypes.getBuiltInPlayerTypes());
    assertThat(
        "All player type labels should be unique, count of unique labels should match total",
        playerTypes.getPlayerTypes().stream().map(PlayerTypes.Type::getLabel).distinct().count(),
        is((long) playerTypes.getPlayerTypes().size()));

    assertThat(
        "No label should be empty ",
        playerTypes.getPlayerTypes().stream()
            .map(PlayerTypes.Type::getLabel)
            .anyMatch(String::isEmpty),
        is(false));
  }
}
