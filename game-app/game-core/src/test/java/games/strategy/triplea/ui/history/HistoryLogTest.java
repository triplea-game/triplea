package games.strategy.triplea.ui.history;

import static games.strategy.triplea.ui.history.HistoryLog.parseHitDifferentialKeyFromDiceRollMessage;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class HistoryLogTest {
  @Nested
  final class ParseHitDifferentialKeyFromDiceRollMessageTest {
    @Test
    void shouldReturnPlayerNameAndRegularDiceTypeWhenMessageContainsRegularDiceRoll() {
      assertThat(
              parseHitDifferentialKeyFromDiceRollMessage(
                  "Russians roll dice for 1 fighter in Karelia S.S.R., round 3 :"))
          .as("player name with only word characters")
          .isEqualTo("Russians regular");
      assertThat(
              parseHitDifferentialKeyFromDiceRollMessage(
                  "West Germans roll dice for 1 fighter in Germany, round 2 :"))
          .as("player name with spaces")
          .isEqualTo("West Germans regular");
    }

    @Test
    void shouldReturnPlayerNameAndAaDiceTypeWhenMessageContainsAaDiceRoll() {
      assertThat(
              parseHitDifferentialKeyFromDiceRollMessage(
                  "Russians roll AA dice in Karelia S.S.R. :"))
          .as("player name and dice type with only word characters")
          .isEqualTo("Russians AA");
      assertThat(
              parseHitDifferentialKeyFromDiceRollMessage("West Germans roll AA dice in Germany :"))
          .as("player name with spaces")
          .isEqualTo("West Germans AA");
      assertThat(
              parseHitDifferentialKeyFromDiceRollMessage(
                  "West Germans roll A.A. dice in Germany :"))
          .as("dice type with non-word characters")
          .isEqualTo("West Germans A.A.");
    }

    @Test
    void shouldReturnMessageWithoutTrailingColonWhenMessageDoesNotContainDiceRoll() {
      assertThat(parseHitDifferentialKeyFromDiceRollMessage("AA fire in Karelia S.S.R."))
          .as("message without trailing colon")
          .isEqualTo("AA fire in Karelia S.S.R.");
      assertThat(parseHitDifferentialKeyFromDiceRollMessage("AA fire in Karelia S.S.R. :"))
          .as("message with trailing colon")
          .isEqualTo("AA fire in Karelia S.S.R.");
    }
  }
}
