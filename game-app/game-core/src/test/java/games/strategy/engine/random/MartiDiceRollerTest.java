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
  private static final String threeDiceSuccess =
      """
      {"status":"OK","result":{"dice":[22,5,100],"signature":"sig","date":123}}
      """;

  private static final String singleDieSuccess =
      """
      {"status":"OK","result":{"dice":[1337]}}
      """;

  private static final String singleError =
      """
      {"status":"Error","errors":["Error description."]}
      """;

  private static final String multipleErrors =
      """
      {"status":"Error","errors":["first","second"]}
      """;

  private static final String zeroDie =
      """
      {"status":"OK","result":{"dice":[0]}}
      """;

  private static final String zeroDieInList =
      """
      {"status":"OK","result":{"dice":[1,0,12]}}
      """;

  private final MartiDiceRoller martiDiceRoller =
      MartiDiceRoller.builder()
          .diceRollerUri(URI.create("http://uri.invalid"))
          .ccAddress("")
          .toAddress("")
          .build();

  @Test
  @SuppressWarnings("unchecked")
  void singleDie() throws Exception {
    final Integer[] dice = boxedArray(martiDiceRoller.getDice(singleDieSuccess, 0));

    assertThat(dice, is(array(equalTo(1336))));
  }

  @Test
  @SuppressWarnings("unchecked")
  void successfulMessageGetsExtractedCorrectly() throws Exception {
    final Integer[] dice = boxedArray(martiDiceRoller.getDice(threeDiceSuccess, 0));

    assertThat(dice, is(array(equalTo(21), equalTo(4), equalTo(99))));
  }

  @Test
  void unsuccessfulMessageGetsExtractedCorrectly() {
    final Exception exception =
        assertThrows(DiceServerException.class, () -> martiDiceRoller.getDice(singleError, 0));
    assertThat(exception.getMessage(), is("Error description."));
  }

  @Test
  void multipleErrorsAreJoined() {
    final Exception exception =
        assertThrows(DiceServerException.class, () -> martiDiceRoller.getDice(multipleErrors, 0));
    assertThat(exception.getMessage(), is("first; second"));
  }

  @Test
  void invalidFormatThrows() {
    assertThrows(IllegalStateException.class, () -> martiDiceRoller.getDice("", 0));
    assertThrows(IllegalStateException.class, () -> martiDiceRoller.getDice("not json", 0));
    assertThrows(IllegalStateException.class, () -> martiDiceRoller.getDice("{}", 0));
    assertThrows(IllegalStateException.class, () -> martiDiceRoller.getDice(zeroDie, 0));
    assertThrows(IllegalStateException.class, () -> martiDiceRoller.getDice(zeroDieInList, 0));
  }

  private static Integer[] boxedArray(final int[] array) {
    return Arrays.stream(array).boxed().toArray(Integer[]::new);
  }
}
