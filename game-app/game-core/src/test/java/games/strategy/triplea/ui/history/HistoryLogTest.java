package games.strategy.triplea.ui.history;

import static games.strategy.triplea.ui.history.HistoryLog.parseHitDifferentialKeyFromDiceRollMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class HistoryLogTest {
  @Nested
  final class ParseHitDifferentialKeyFromDiceRollMessageTest {
    @Test
    void shouldReturnPlayerNameAndRegularDiceTypeWhenMessageContainsRegularDiceRoll() {
      assertThat(
          "player name with only word characters",
          parseHitDifferentialKeyFromDiceRollMessage(
              "Russians roll dice for 1 fighter in Karelia S.S.R., round 3 :"),
          is("Russians regular"));
      assertThat(
          "player name with spaces",
          parseHitDifferentialKeyFromDiceRollMessage(
              "West Germans roll dice for 1 fighter in Germany, round 2 :"),
          is("West Germans regular"));
    }

    @Test
    void shouldReturnPlayerNameAndAaDiceTypeWhenMessageContainsAaDiceRoll() {
      assertThat(
          "player name and dice type with only word characters",
          parseHitDifferentialKeyFromDiceRollMessage("Russians roll AA dice in Karelia S.S.R. :"),
          is("Russians AA"));
      assertThat(
          "player name with spaces",
          parseHitDifferentialKeyFromDiceRollMessage("West Germans roll AA dice in Germany :"),
          is("West Germans AA"));
      assertThat(
          "dice type with non-word characters",
          parseHitDifferentialKeyFromDiceRollMessage("West Germans roll A.A. dice in Germany :"),
          is("West Germans A.A."));
    }

    @Test
    void shouldReturnMessageWithoutTrailingColonWhenMessageDoesNotContainDiceRoll() {
      assertThat(
          "message without trailing colon",
          parseHitDifferentialKeyFromDiceRollMessage("AA fire in Karelia S.S.R."),
          is("AA fire in Karelia S.S.R."));
      assertThat(
          "message with trailing colon",
          parseHitDifferentialKeyFromDiceRollMessage("AA fire in Karelia S.S.R. :"),
          is("AA fire in Karelia S.S.R."));
    }
  }
}
