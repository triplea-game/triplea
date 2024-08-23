package games.strategy.net;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserNameAssignerTest {

  @NonNls private static final String NAME_1 = "name_one";
  @NonNls private static final String NAME_2 = "name_two";

  @NonNls private static final String MAC = "mac 1";

  /**
   * Null for IP address or node list means we have something wrong on the server side and should
   * see an exception.
   */
  @Test
  void errorCasesWithNullArguments() {
    assertThrows(
        NullPointerException.class, () -> UserNameAssigner.assignName(NAME_1, null, Set.of()));

    assertThrows(NullPointerException.class, () -> UserNameAssigner.assignName(NAME_1, MAC, null));
  }

  @Test
  void assignNameShouldGetAssignedNameWhenNotTaken() {
    assertThat(
        "no nodes to match against, we should get the desired name",
        UserNameAssigner.assignName(NAME_1, MAC, Set.of()),
        is(NAME_1));

    assertThat(
        "name and address do not match, should get the desired name",
        UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_2)),
        is(NAME_1));
  }

  @Test
  void assignNameWithMatchingNames() {
    assertThat(
        "name match, should be assigned a numeral name",
        UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_1)),
        is(NAME_1 + " (1)"));

    assertThat(
        "name match, matching against multiple nodes",
        UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_2, NAME_1)),
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
        UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_1, NAME_1 + " (1)")),
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
        UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_1 + " (1)")),
        is(NAME_1));

    assertThat(
        "name matches and there is gap in numbering",
        UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_1, NAME_1 + " (2)")),
        is(NAME_1 + " (1)"));

    assertThat(
        "name matches and there is gap in numbering, ordering should not matter",
        UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_1 + " (3)", NAME_1 + " (1)", NAME_1)),
        is(NAME_1 + " (2)"));

    assertThat(
        "should get next ascending numeral",
        UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_1 + " (2)", NAME_1 + " (1)", NAME_1)),
        is(NAME_1 + " (3)"));
  }
}
