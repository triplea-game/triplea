package games.strategy.net;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.common.LobbyConstants;

@ExtendWith(MockitoExtension.class)
class PlayerNameAssignerTest {

  private static final String NAME_1 = "name_one";
  private static final String NAME_2 = "name_two";

  private static final String MAC = "mac 1";

  /**
   * Null for IP address or node list means we have something wrong on the server side and should
   * see an exception.
   */
  @Test
  void errorCasesWithNullArguments() {
    assertThrows(
        NullPointerException.class, () -> PlayerNameAssigner.assignName(NAME_1, null, emptySet()));

    assertThrows(
        NullPointerException.class, () -> PlayerNameAssigner.assignName(NAME_1, MAC, null));
  }

  @Test
  void assignNameShouldGetAssignedNameWhenNotTaken() {
    assertThat(
        "no nodes to match against, we should get the desired name",
        PlayerNameAssigner.assignName(NAME_1, MAC, emptySet()),
        is(NAME_1));

    assertThat(
        "name and address do not match, should get the desired name",
        PlayerNameAssigner.assignName(NAME_1, MAC, singletonList(NAME_2)),
        is(NAME_1));
  }

  @Test
  void assignNameWithMatchingNames() {
    assertThat(
        "name match, should be assigned a numeral name",
        PlayerNameAssigner.assignName(NAME_1, MAC, singletonList(NAME_1)),
        is(NAME_1 + " (1)"));

    assertThat(
        "name match, matching against multiple nodes",
        PlayerNameAssigner.assignName(NAME_1, MAC, asList(NAME_2, NAME_1)),
        is(NAME_1 + " (1)"));
  }

  /**
   * Verifies that when we have multiple names differing by numeral that we'll get the next
   * available numeral.
   */
  @Test
  void assignNameMultipleNumerals() {
    assertThat(
        "name match, should get next sequential numeral appended",
        PlayerNameAssigner.assignName(NAME_1, MAC, asList(NAME_1, NAME_1 + " (1)")),
        is(NAME_1 + " (2)"));
  }

  /**
   * If we have "name", and "name (2)", the next value should be "name (1)" before we get "name
   * (3)".
   */
  @Test
  void assignNameShouldFillInMissingNumerals() {
    assertThat(
        "name does not actually match",
        PlayerNameAssigner.assignName(NAME_1, MAC, singletonList(NAME_1 + " (1)")),
        is(NAME_1));

    assertThat(
        "name matches and there is gap in numbering",
        PlayerNameAssigner.assignName(NAME_1, MAC, asList(NAME_1, NAME_1 + " (2)")),
        is(NAME_1 + " (1)"));

    assertThat(
        "name matches and there is gap in numbering, ordering should not matter",
        PlayerNameAssigner.assignName(
            NAME_1, MAC, asList(NAME_1 + " (3)", NAME_1 + " (1)", NAME_1)),
        is(NAME_1 + " (2)"));

    assertThat(
        "should get next ascending numeral",
        PlayerNameAssigner.assignName(
            NAME_1, MAC, asList(NAME_1 + " (2)", NAME_1 + " (1)", NAME_1)),
        is(NAME_1 + " (3)"));
  }

  @Test
  void multipleBotNames() {
    final String bot01 = "Bot01_bot" + LobbyConstants.LOBBY_WATCHER_NAME;
    final String bot02 = "Bot02_bot" + LobbyConstants.LOBBY_WATCHER_NAME;
    final String bot03 = "Bot02_bot" + LobbyConstants.LOBBY_WATCHER_NAME;

    assertThat(
        "with a bot logged in already, mac check should ignore the already logged in bot",
        PlayerNameAssigner.assignName(bot01, MAC, singletonList(bot02)),
        is(bot01));

    assertThat(
        "again, even with multiple bots logged in, mac check should ignore existing logins "
            + "that have a bot lobby watch name",
        PlayerNameAssigner.assignName(bot01, MAC, asList(bot02, bot03)),
        is(bot01));
  }
}
