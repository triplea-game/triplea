package games.strategy.engine.framework.startup.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import games.strategy.engine.player.Player;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerTypesTest {

  @Test
  void playerTypes() {
    final PlayerTypes playerTypes = new PlayerTypes(PlayerTypes.getBuiltInPlayerTypes());
    assertThat(List.of(playerTypes.getAvailablePlayerLabels()))
        .as("Ensure we have a visible player type in the selection list")
        .contains(PlayerTypes.WEAK_AI.getLabel());
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
              assertThat(result.getPlayerLabel())
                  .as("The player label should match after construction, input type: " + playerType)
                  .isEqualTo(playerType.getLabel());
              assertThat(result.getName())
                  .as(
                      "The name is a passed in parameter, this should still match after construction")
                  .isEqualTo(testName);
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
                assertThat(playerTypesProvider.fromLabel(playerType.getLabel()))
                    .as("Make sure that we can reconstruct each player type from its label")
                    .isEqualTo(playerType));
  }

  @Test
  void getLabel() {
    final PlayerTypes playerTypes = new PlayerTypes(PlayerTypes.getBuiltInPlayerTypes());
    assertThat(
            playerTypes.getPlayerTypes().stream()
                .map(PlayerTypes.Type::getLabel)
                .distinct()
                .count())
        .as("All player type labels should be unique, count of unique labels should match total")
        .isEqualTo((long) playerTypes.getPlayerTypes().size());

    assertThat(
            playerTypes.getPlayerTypes().stream()
                .map(PlayerTypes.Type::getLabel)
                .anyMatch(String::isEmpty))
        .as("No label should be empty ")
        .isFalse();
  }
}
