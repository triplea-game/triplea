package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.triplea.util.Tuple;

final class GamePlayerTest {
  private final GamePlayer gamePlayer = new GamePlayer("name", new GameData());

  @Nested
  final class GetPlayerTypesTest {
    @Test
    void shouldReturnType() {
      List.of(Tuple.of("AI", "Hard (AI)"), Tuple.of("Human", "Patton"), Tuple.of("null", "Bot"))
          .forEach(
              idAndName -> {
                gamePlayer.setWhoAmI(idAndName.getFirst() + ":" + idAndName.getSecond());

                assertThat(
                    gamePlayer.getPlayerType(),
                    is(new GamePlayer.Type(idAndName.getFirst(), idAndName.getSecond())));
              });
    }
  }

  @Nested
  final class IsAiTest {
    @Test
    void shouldReturnTrueWhenTypeIsAi() {
      List.of("AI:Hard (AI)", "ai:hard (ai)")
          .forEach(
              encodedType -> {
                gamePlayer.setWhoAmI(encodedType);

                assertThat(gamePlayer.isAi(), is(true));
              });
    }

    @Test
    void shouldReturnFalseWhenTypeIsNotAi() {
      List.of("Human:Patton", "null:Bot")
          .forEach(
              encodedType -> {
                gamePlayer.setWhoAmI(encodedType);

                assertThat(gamePlayer.isAi(), is(false));
              });
    }
  }

  @Nested
  final class SetWhoAmITest {
    @Test
    void shouldSetWhoAmIWhenEncodedTypeIsLegal() {
      List.of(
              "AI:Hard (AI)",
              "ai:Hard (AI)",
              "Human:Patton",
              "huMAN:Patton",
              "null:Bot",
              "NulL:Bot")
          .forEach(
              encodedType -> {
                gamePlayer.setWhoAmI(encodedType);

                assertThat(gamePlayer.getWhoAmI(), is(encodedType));
              });
    }

    @Test
    void shouldThrowExceptionWhenEncodedTypeDoesNotContainExactlyTwoTokens() {
      assertThrowsDoesNotHaveExactlyTwoTokensException(() -> gamePlayer.setWhoAmI("Patton"));
      assertThrowsDoesNotHaveExactlyTwoTokensException(
          () -> gamePlayer.setWhoAmI("Human:Patton:Third"));
    }

    private void assertThrowsDoesNotHaveExactlyTwoTokensException(final Executable executable) {
      final Exception e = assertThrows(IllegalArgumentException.class, executable);
      assertThat(e.getMessage(), containsString("two strings"));
    }

    @Test
    void shouldThrowExceptionWhenTypeIdIsIllegal() {
      final Exception e =
          assertThrows(
              IllegalArgumentException.class, () -> gamePlayer.setWhoAmI("otherTypeId:Patton"));
      assertThat(e.getMessage(), containsString("ai or human or null"));
    }
  }

  @Nested
  final class TypeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      EqualsVerifier.forClass(GamePlayer.Type.class).verify();
    }
  }
}
