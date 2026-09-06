package games.strategy.net;

import static org.assertj.core.api.Assertions.assertThat;
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
    assertThat(UserNameAssigner.assignName(NAME_1, MAC, Set.of()))
        .as("no nodes to match against, we should get the desired name")
        .isEqualTo(NAME_1);

    assertThat(UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_2)))
        .as("name and address do not match, should get the desired name")
        .isEqualTo(NAME_1);
  }

  @Test
  void assignNameWithMatchingNames() {
    assertThat(UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_1)))
        .as("name match, should be assigned a numeral name")
        .isEqualTo(NAME_1 + " (1)");

    assertThat(UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_2, NAME_1)))
        .as("name match, matching against multiple nodes")
        .isEqualTo(NAME_1 + " (1)");
  }

  /**
   * Verifies that when we have multiple names differing by numeral that we'll get the next
   * available numeral.
   */
  @Test
  void assignNameMultipleNumerals() {
    assertThat(UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_1, NAME_1 + " (1)")))
        .as("name match, should get next sequential numeral appended")
        .isEqualTo(NAME_1 + " (2)");
  }

  /**
   * If we have "name", and "name (2)", the next value should be "name (1)" before we get "name
   * (3)".
   */
  @Test
  void assignNameShouldFillInMissingNumerals() {
    assertThat(UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_1 + " (1)")))
        .as("name does not actually match")
        .isEqualTo(NAME_1);

    assertThat(UserNameAssigner.assignName(NAME_1, MAC, List.of(NAME_1, NAME_1 + " (2)")))
        .as("name matches and there is gap in numbering")
        .isEqualTo(NAME_1 + " (1)");

    assertThat(
            UserNameAssigner.assignName(
                NAME_1, MAC, List.of(NAME_1 + " (3)", NAME_1 + " (1)", NAME_1)))
        .as("name matches and there is gap in numbering, ordering should not matter")
        .isEqualTo(NAME_1 + " (2)");

    assertThat(
            UserNameAssigner.assignName(
                NAME_1, MAC, List.of(NAME_1 + " (2)", NAME_1 + " (1)", NAME_1)))
        .as("should get next ascending numeral")
        .isEqualTo(NAME_1 + " (3)");
  }
}
