package games.strategy.engine.random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsArray.array;
import static org.junit.jupiter.api.Assertions.assertThrows;

import games.strategy.engine.random.IRemoteDiceServer.DiceServerException;
import java.net.URI;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class MartiDiceRollerTest {
  private static final String successMessage =
      "Some text before<br>your dice are: 2,5,1<p><p>\t</body>\nsome text after";

  private static final String failureMessage =
      "some text before\t\tfatal error: Error description.!\nsome text after";

  private final MartiDiceRoller martiDiceRoller =
      MartiDiceRoller.builder()
          .diceRollerUri(URI.create("http://uri.invalid"))
          .ccAddress("")
          .toAddress("")
          .gameId("")
          .build();

  @Test
  @SuppressWarnings("unchecked")
  void successfulMessageGetsExtractedCorrectly() throws Exception {
    final Integer[] dice =
        Arrays.stream(martiDiceRoller.getDice(successMessage, 0)).boxed().toArray(Integer[]::new);

    assertThat(dice, is(array(equalTo(1), equalTo(4), equalTo(0))));
  }

  @Test
  void unsuccessfulMessageGetsExtractedCorrectly() {
    final Exception exception =
        assertThrows(DiceServerException.class, () -> martiDiceRoller.getDice(failureMessage, 0));
    assertThat(exception.getMessage(), is(" Error description."));
  }

  @Test
  void verifyCorrectMissingOrInvalidTokenHandling() {
    assertThrows(IllegalStateException.class, () -> martiDiceRoller.getDice("", 0));
    assertThrows(IllegalStateException.class, () -> martiDiceRoller.getDice("your dice are:", 0));
    assertThrows(IllegalStateException.class, () -> martiDiceRoller.getDice("<p>", 0));
    assertThrows(
        IllegalStateException.class, () -> martiDiceRoller.getDice("<p>your dice are:", 0));
    assertThrows(
        IllegalStateException.class, () -> martiDiceRoller.getDice("your dice are:NaN<p>", 0));
  }
}
