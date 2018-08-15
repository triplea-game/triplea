package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import games.strategy.util.Tuple;

final class PlayerIdTest {
  private final PlayerID playerId = new PlayerID("name", new GameData());

  @Nested
  final class GetTypeAndNameTest {
    @Test
    void shouldReturnTypeAndName() {
      Arrays.asList(
          Tuple.of("AI", "Hard (AI)"),
          Tuple.of("Human", "Patton"),
          Tuple.of("null", "Bot")).forEach(typeAndName -> {
            playerId.setWhoAmI(typeAndName.getFirst() + ":" + typeAndName.getSecond());

            assertThat(playerId.getTypeAndName(), is(typeAndName));
          });
    }
  }

  @Nested
  final class IsAiTest {
    @Test
    void shouldReturnTrueWhenTypeIsAi() {
      Arrays.asList(
          "AI:Hard (AI)",
          "ai:hard (ai)").forEach(encodedTypeAndName -> {
            playerId.setWhoAmI(encodedTypeAndName);

            assertThat(playerId.isAi(), is(true));
          });
    }

    @Test
    void shouldReturnFalseWhenTypeIsNotAi() {
      Arrays.asList(
          "Human:Patton",
          "null:Bot").forEach(encodedTypeAndName -> {
            playerId.setWhoAmI(encodedTypeAndName);

            assertThat(playerId.isAi(), is(false));
          });
    }
  }

  @Nested
  final class SetWhoAmITest {
    @Test
    void shouldSetWhoAmIWhenEncodedValueIsLegal() {
      Arrays.asList(
          "AI:Hard (AI)",
          "ai:Hard (AI)",
          "Human:Patton",
          "huMAN:Patton",
          "null:Bot",
          "NulL:Bot").forEach(encodedTypeAndName -> {
            playerId.setWhoAmI(encodedTypeAndName);

            assertThat(playerId.getWhoAmI(), is(encodedTypeAndName));
          });
    }

    @Test
    void shouldThrowExceptionWhenEncodedValueDoesNotContainExactlyTwoTokens() {
      assertThrowsDoesNotHaveExactlyTwoTokensException(() -> playerId.setWhoAmI("Patton"));
      assertThrowsDoesNotHaveExactlyTwoTokensException(() -> playerId.setWhoAmI("Human:Patton:Third"));
    }

    private void assertThrowsDoesNotHaveExactlyTwoTokensException(final Executable executable) {
      final Exception e = assertThrows(IllegalStateException.class, executable);
      assertThat(e.getMessage(), containsString("two strings"));
    }

    @Test
    void shouldThrowExceptionWhenTypeIsIllegal() {
      final Exception e = assertThrows(IllegalStateException.class, () -> playerId.setWhoAmI("otherType:Patton"));
      assertThat(e.getMessage(), containsString("ai or human or null"));
    }
  }
}
