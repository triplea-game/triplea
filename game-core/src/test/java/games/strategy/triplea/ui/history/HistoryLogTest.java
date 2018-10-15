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
          parseHitDifferentialKeyFromDiceRollMessage("Russians roll dice for 1 fighter in Karelia S.S.R., round 3 :"),
          is("Russians regular"));
    }

    @Test
    void shouldReturnPlayerNameAndAaDiceTypeWhenMessageContainsAaDiceRoll() {
      assertThat(
          parseHitDifferentialKeyFromDiceRollMessage("Russians roll AA dice in Karelia S.S.R. :"),
          is("Russians AA"));
    }

    @Test
    void shouldReturnMessageWithoutTrailingColonWhenMessageDoesNotContainDiceRoll() {
      assertThat(
          parseHitDifferentialKeyFromDiceRollMessage("AA fire in Karelia S.S.R."),
          is("AA fire in Karelia S.S.R."));
      assertThat(
          parseHitDifferentialKeyFromDiceRollMessage("AA fire in Karelia S.S.R. :"),
          is("AA fire in Karelia S.S.R."));
    }
  }
}
