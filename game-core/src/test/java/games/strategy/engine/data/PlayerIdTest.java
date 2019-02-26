package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.triplea.util.Tuple;

import nl.jqno.equalsverifier.EqualsVerifier;

final class PlayerIdTest {
  private final PlayerId playerId = new PlayerId("name", new GameData());

  @Nested
  final class GetPlayerTypeTest {
    @Test
    void shouldReturnType() {
      Arrays.asList(
          Tuple.of("AI", "Hard (AI)"),
          Tuple.of("Human", "Patton"),
          Tuple.of("null", "Bot")).forEach(idAndName -> {
            playerId.setWhoAmI(idAndName.getFirst() + ":" + idAndName.getSecond());

            assertThat(playerId.getPlayerType(), is(new PlayerId.Type(idAndName.getFirst(), idAndName.getSecond())));
          });
    }
  }

  @Nested
  final class IsAiTest {
    @Test
    void shouldReturnTrueWhenTypeIsAi() {
      Arrays.asList(
          "AI:Hard (AI)",
          "ai:hard (ai)").forEach(encodedType -> {
            playerId.setWhoAmI(encodedType);

            assertThat(playerId.isAi(), is(true));
          });
    }

    @Test
    void shouldReturnFalseWhenTypeIsNotAi() {
      Arrays.asList(
          "Human:Patton",
          "null:Bot").forEach(encodedType -> {
            playerId.setWhoAmI(encodedType);

            assertThat(playerId.isAi(), is(false));
          });
    }
  }

  @Nested
  final class SetWhoAmITest {
    @Test
    void shouldSetWhoAmIWhenEncodedTypeIsLegal() {
      Arrays.asList(
          "AI:Hard (AI)",
          "ai:Hard (AI)",
          "Human:Patton",
          "huMAN:Patton",
          "null:Bot",
          "NulL:Bot").forEach(encodedType -> {
            playerId.setWhoAmI(encodedType);

            assertThat(playerId.getWhoAmI(), is(encodedType));
          });
    }

    @Test
    void shouldThrowExceptionWhenEncodedTypeDoesNotContainExactlyTwoTokens() {
      assertThrowsDoesNotHaveExactlyTwoTokensException(() -> playerId.setWhoAmI("Patton"));
      assertThrowsDoesNotHaveExactlyTwoTokensException(() -> playerId.setWhoAmI("Human:Patton:Third"));
    }

    private void assertThrowsDoesNotHaveExactlyTwoTokensException(final Executable executable) {
      final Exception e = assertThrows(IllegalArgumentException.class, executable);
      assertThat(e.getMessage(), containsString("two strings"));
    }

    @Test
    void shouldThrowExceptionWhenTypeIdIsIllegal() {
      final Exception e = assertThrows(IllegalArgumentException.class, () -> playerId.setWhoAmI("otherTypeId:Patton"));
      assertThat(e.getMessage(), containsString("ai or human or null"));
    }
  }

  @Nested
  final class TypeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      EqualsVerifier.forClass(PlayerId.Type.class).verify();
    }
  }
}
