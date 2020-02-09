package games.strategy.engine.random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsArray.array;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class MartiDiceRollerTest {
  private static final String successMessage =
      "<!DOCTYPE html>\n"
          + "<html>\n"
          + "\t<head>\n"
          + "\t\t<title>M.A.R.T.I. Server -- \"more accurate rolls than irony\"</title>\n"
          + "\t</head>\n"
          + "\t<body>\n"
          + "\t\t<p>Dice results were sent via email.</p>"
          + "<br><a href='https://dice.marti.triplea-game.org/MARTI_verify.php?iv=oTVwKksuDf4f0LW4j"
          + "6U6yAklQWsxpdac%2FmX1NMP7uXY%3D&enc=vhDa64l%2FORX6X8di6LVdl5JQGC81uQyKE4Jt0gHoVsOV5Uxz"
          + "msbVReOsBQonZmQi7JZAwGhL4HoFXzkJMjilNX1gRMs5%2FhXyjyoUryYGPzgOWIVG7IxkuOMRiot7kPqJQwNc"
          + "f08ev8TNj9WxZTSWPNy2j1Y17piXRn%2FffXIGPDDcrmw2rSeB3lCq6ZjlKSmMCIqEXk%2FSsJjQO7jKq7CkWT"
          + "ZPY2QxTfFuOmFEj1Ji7pp1GVuVcc5g90q5'>Click here to verify the roll.</a>"
          + "<br>your dice are: 2,5,1<p><p>\t</body>\n"
          + "</html>";

  private static final String failureMessage =
      "<!DOCTYPE html>\n"
          + "<html>\n"
          + "\t<head>\n"
          + "\t\t<title>M.A.R.T.I. Server -- \"more accurate rolls than irony\"</title>\n"
          + "\t</head>\n"
          + "\t<body>\n"
          + "\t\tfatal error: Emails [user@mail.invalid] are not registered."
          + " Please register them at https://dice.marti.triplea-game.org/register.php .!\n"
          + "\n";

  private final MartiDiceRoller martiDiceRoller =
      MartiDiceRoller.builder()
          .diceRollerUri(URI.create("http://uri.invalid"))
          .ccAddress("")
          .toAddress("")
          .gameId("")
          .build();

  @Test
  @SuppressWarnings("unchecked")
  void successfulMessageExtraction() throws Exception {
    final Integer[] dice =
        Arrays.stream(martiDiceRoller.getDice(successMessage, 0)).boxed().toArray(Integer[]::new);

    assertThat(dice, is(array(equalTo(1), equalTo(4), equalTo(0))));
  }

  @Test
  void unsuccessfulMessageExtraction() {
    final Exception exception =
        assertThrows(
            InvocationTargetException.class, () -> martiDiceRoller.getDice(failureMessage, 0));
    assertThat(
        exception.getMessage(),
        is(
            " Emails [user@mail.invalid] are not registered."
                + " Please register them at https://dice.marti.triplea-game.org/register.php ."));
    assertThat(exception.getCause(), is(nullValue()));
  }

  @Test
  void verifyCorrectMissingOrInvalidTokenHandling() {
    assertThrows(IOException.class, () -> martiDiceRoller.getDice("", 0));
    assertThrows(IOException.class, () -> martiDiceRoller.getDice("your dice are:", 0));
    assertThrows(IOException.class, () -> martiDiceRoller.getDice("<p>", 0));
    assertThrows(IOException.class, () -> martiDiceRoller.getDice("<p>your dice are:", 0));
    assertThrows(IOException.class, () -> martiDiceRoller.getDice("your dice are:NaN<p>", 0));
  }
}
