package games.strategy.engine.random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsArray.array;
import static org.junit.jupiter.api.Assertions.assertThrows;

import games.strategy.engine.random.IRemoteDiceServer.DiceServerException;
import java.net.URI;
import java.util.Arrays;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MartiDiceRollerTest {
  private static final String successMessage =
      "Some text before<br>your dice are: 22,5,100<p><p>\t</body>\nsome text after";

  private static final String failureMessage =
      "some text before\t\tfatal error: Error description.!\nsome text after";

  private final MartiDiceRoller martiDiceRoller =
      MartiDiceRoller.builder()
          .diceRollerUri(URI.create("http://uri.invalid"))
          .ccAddress("")
          .toAddress("")
          .gameId("")
          .build();

  @Nested
  class SuccessPatterns {

    @Test
    @SuppressWarnings("unchecked")
    void singleDieSingleDigit() throws Exception {
      final Integer[] dice = boxedArray(martiDiceRoller.getDice("your dice are: 1<p>", 0));

      assertThat(dice, is(array(equalTo(0))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void singleDieMultipleDigit() throws Exception {
      final Integer[] dice = boxedArray(martiDiceRoller.getDice("your dice are: 1337<p>", 0));

      assertThat(dice, is(array(equalTo(1336))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void onlyFirstDieSingleDigit() throws Exception {
      final Integer[] dice = boxedArray(martiDiceRoller.getDice("your dice are: 1,42,256<p>", 0));

      assertThat(dice, is(array(equalTo(0), equalTo(41), equalTo(255))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void onlyFirstDieMultipleDigit() throws Exception {
      final Integer[] dice = boxedArray(martiDiceRoller.getDice("your dice are: 512,3,4,5<p>", 0));

      assertThat(dice, is(array(equalTo(511), equalTo(2), equalTo(3), equalTo(4))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void mixedDigitCount() throws Exception {
      final Integer[] dice = boxedArray(martiDiceRoller.getDice("your dice are: 22,1,333<p>", 0));

      assertThat(dice, is(array(equalTo(21), equalTo(0), equalTo(332))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void leadingZeroes() throws Exception {
      final Integer[] dice =
          boxedArray(martiDiceRoller.getDice("your dice are: 01,002,0003<p>", 0));

      assertThat(dice, is(array(equalTo(0), equalTo(1), equalTo(2))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void twoDice() throws Exception {
      final Integer[] dice = boxedArray(martiDiceRoller.getDice("your dice are: 1,2<p>", 0));

      assertThat(dice, is(array(equalTo(0), equalTo(1))));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void successfulMessageGetsExtractedCorrectly() throws Exception {
    final Integer[] dice = boxedArray(martiDiceRoller.getDice(successMessage, 0));

    assertThat(dice, is(array(equalTo(21), equalTo(4), equalTo(99))));
  }

  private static Integer[] boxedArray(int[] array) {
    return Arrays.stream(array).boxed().toArray(Integer[]::new);
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
    assertThrows(
        IllegalStateException.class, () -> martiDiceRoller.getDice("your dice are: 0<p>", 0));
    assertThrows(
        IllegalStateException.class, () -> martiDiceRoller.getDice("your dice are: 00<p>", 0));
    assertThrows(
        IllegalStateException.class, () -> martiDiceRoller.getDice("your dice are: 0,0<p>", 0));
    assertThrows(
        IllegalStateException.class, () -> martiDiceRoller.getDice("your dice are: 00,1<p>", 0));
    assertThrows(
        IllegalStateException.class,
        () -> martiDiceRoller.getDice("your dice are: 00,10,12<p>", 0));
    assertThrows(
        IllegalStateException.class, () -> martiDiceRoller.getDice("your dice are: 1,0,12<p>", 0));
    assertThrows(
        IllegalStateException.class, () -> martiDiceRoller.getDice("your dice are: 1,0,0<p>", 0));
    assertThrows(
        IllegalStateException.class, () -> martiDiceRoller.getDice("your dice are: 1,1,0<p>", 0));
  }
}
