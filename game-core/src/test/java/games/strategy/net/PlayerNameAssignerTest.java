package games.strategy.net;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetAddress;
import java.util.Arrays;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerNameAssignerTest {

  private static final String name1 = "Never endure a plunder.";
  private static final String name2 = "C'mon, never vandalize a gull";

  @Mock private InetAddress address1;

  @Mock private InetAddress address2;

  @Nested
  final class ErrorCases {
    /**
     * Null for IP address or node list means we have something wrong on the server side and should
     * see an exception.
     */
    @Test
    void nullArguments() {
      assertThrows(
          NullPointerException.class,
          () -> PlayerNameAssigner.assignName(name1, null, emptyList()));

      assertThrows(
          NullPointerException.class, () -> PlayerNameAssigner.assignName(name1, address1, null));
    }

    /** Short or bad name could be a custom (hacked) client. */
    @Test
    void nameTooShort() {
      Arrays.asList(null, "", "a", "_")
          .forEach(
              nameTooShort ->
                  assertThat(
                      "With a name too short, we could have a hacked client, should use a default "
                          + "dummy name and not error out on the server side.",
                      PlayerNameAssigner.assignName(nameTooShort, address1, emptyList()),
                      is(PlayerNameAssigner.DUMMY_NAME)));
    }

    @Test
    void badNamesPassedFromClientWithDuplicate() {
      assertThat(
          "We already have a dummy name, so we should see an increment to the name.",
          PlayerNameAssigner.assignName(
              null, address1, singleton(createNode(PlayerNameAssigner.DUMMY_NAME, address2))),
          is(PlayerNameAssigner.DUMMY_NAME + " (1)"));

      assertThat(
          "We already have a couple dummy names, should see an increment to the name.",
          PlayerNameAssigner.assignName(
              null,
              address1,
              asList(
                  createNode(PlayerNameAssigner.DUMMY_NAME, address2),
                  createNode(PlayerNameAssigner.DUMMY_NAME + " (1)", address2))),
          is(PlayerNameAssigner.DUMMY_NAME + " (2)"));
    }
  }

  @Test
  void assignNameShouldGetAssignedNameWhenNotTaken() {
    assertThat(
        "no nodes to match against, we should get the desired name",
        PlayerNameAssigner.assignName(name1, address1, emptyList()),
        is(name1));

    assertThat(
        "name and address do not match, should get the desired name",
        PlayerNameAssigner.assignName(name1, address1, singleton(createNode(name2, address2))),
        is(name1));
  }

  @Test
  void assignNameWithMatchingNames() {
    assertThat(
        "name match, should be assigned a numeral name",
        PlayerNameAssigner.assignName(name1, address1, singleton(createNode(name1, address2))),
        is(name1 + " (1)"));

    assertThat(
        "name match, matching against multiple nodes",
        PlayerNameAssigner.assignName(
            name1, address1, asList(createNode(name2, address2), createNode(name1, address2))),
        is(name1 + " (1)"));
  }

  /**
   * Verifies that when we have multiple names differing by numeral that we'll get the next
   * available numeral.
   */
  @Test
  void assignNameMultipleNumerals() {
    assertThat(
        "name match, should get next sequential numeral appended",
        PlayerNameAssigner.assignName(
            name1,
            address1,
            asList(createNode(name1, address2), createNode(name1 + " (1)", address2))),
        is(name1 + " (2)"));

    assertThat(
        "name match, should get next sequential numeral appended, " + "ordering should not matter",
        PlayerNameAssigner.assignName(
            name1,
            address1,
            asList(createNode(name1 + " (1)", address2), createNode(name1, address2))),
        is(name1 + " (2)"));
  }

  /**
   * If we have "name", and "name (2)", the next value should be "name (1)" before we get "name
   * (3)".
   */
  @Test
  void assignNameShouldFillInMissingNumerals() {
    assertThat(
        "name does not actually match",
        PlayerNameAssigner.assignName(
            name1, address1, singleton(createNode(name1 + " (1)", address2))),
        is(name1));

    assertThat(
        "name matches and there is gap in numbering",
        PlayerNameAssigner.assignName(
            name1,
            address1,
            asList(createNode(name1, address2), createNode(name1 + " (2)", address2))),
        is(name1 + " (1)"));

    assertThat(
        "name matches and there is gap in numbering, ordering should not matter",
        PlayerNameAssigner.assignName(
            name1,
            address1,
            asList(
                createNode(name1 + " (3)", address2),
                createNode(name1 + " (1)", address2),
                createNode(name1, address2))),
        is(name1 + " (2)"));

    assertThat(
        "should get next ascending numeral",
        PlayerNameAssigner.assignName(
            name1,
            address1,
            asList(
                createNode(name1 + " (2)", address2),
                createNode(name1 + " (1)", address2),
                createNode(name1, address2))),
        is(name1 + " (3)"));
  }

  /**
   * If a user choose a name like "name (1)", then the numeral append should still work and append
   * to that name, eg: "name (1) (1)".
   */
  @Test
  void doubleAppendCase() {
    assertThat(
        "if requesting a name ending in (1), and we have that already, then append again",
        PlayerNameAssigner.assignName(
            name1 + " (1)", address1, singleton(createNode(name1 + " (1)", address2))),
        is(name1 + " (1) (1)"));

    assertThat(
        "if requesting a name ending in (1), and we have that already, then append again,"
            + "and verify incrementing numerals",
        PlayerNameAssigner.assignName(
            name1 + " (1)",
            address1,
            asList(
                createNode(name1 + " (1) (2)", address2),
                createNode(name1 + " (1)", address2),
                createNode(name1 + " (1) (1)", address2))),
        is(name1 + " (1) (3)"));

    assertThat(
        "oddball case where there is a matching substring, we need a full match",
        PlayerNameAssigner.assignName(
            name1 + " (1)", address1, singleton(createNode(name1, address2))),
        is(name1 + " (1)"));
  }

  /**
   * Not only will matching names cause a numeral increment, but if we see the same address then
   * we'll ignore the requested name and append a numeral increment.
   */
  @Test
  void matchingAddressWillIncrementOriginalName() {
    assertThat(
        "addresses match",
        PlayerNameAssigner.assignName(name1, address1, singleton(createNode(name2, address1))),
        is(name2 + " (1)"));

    assertThat(
        "addresses match, ordering should not matter",
        PlayerNameAssigner.assignName(
            name1,
            address1,
            asList(createNode(name2 + " (1)", address1), createNode(name2, address1))),
        is(name2 + " (2)"));

    assertThat(
        "addresses match, should fill in gaps in the numerals",
        PlayerNameAssigner.assignName(
            name1,
            address1,
            asList(createNode(name2 + " (2)", address1), createNode(name2, address1))),
        is(name2 + " (1)"));
  }

  private static INode createNode(final String name, final InetAddress inetAddress) {
    return new Node(name, inetAddress, 1234);
  }
}
