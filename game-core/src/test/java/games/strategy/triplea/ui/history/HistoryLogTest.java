package games.strategy.triplea.ui.history;

import static games.strategy.triplea.ui.history.HistoryLog.parsePlayerNameFromDiceRollMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class HistoryLogTest {
  @Nested
  final class ParsePlayerNameFromDiceRollMessageTest {

    @Test
    void shouldParsePlayerName() {
      assertThat(
          parsePlayerNameFromDiceRollMessage("Japanese roll dice for 1 battleship in Australia, round 2 :"),
          is("Japanese"));
    }

    @Test
    void shouldParsePlayerNameWithoutRoll() {
      assertThat(
          parsePlayerNameFromDiceRollMessage("Japanese"),
          is("Japanese"));
    }
  }
}
