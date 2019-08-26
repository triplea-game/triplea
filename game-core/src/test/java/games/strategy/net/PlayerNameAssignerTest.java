package games.strategy.net;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerNameAssignerTest {

  private static final String NAME_1 = "name_one";
  private static final String NAME_2 = "name_two";

  private static final String MAC = "mac 1";
  private static final String MAC_2 = "mac 2";

  //  @Mock private InetAddress address1;

  //  @Mock private InetAddress address2;

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
          () -> PlayerNameAssigner.assignName(NAME_1, null, HashMultimap.create()));

      assertThrows(
          NullPointerException.class, () -> PlayerNameAssigner.assignName(NAME_1, MAC, null));
    }
  }

  @Test
  void assignNameShouldGetAssignedNameWhenNotTaken() {
    assertThat(
        "no nodes to match against, we should get the desired name",
        PlayerNameAssigner.assignName(NAME_1, MAC, HashMultimap.create()),
        is(NAME_1));

    assertThat(
        "name and address do not match, should get the desired name",
        PlayerNameAssigner.assignName(NAME_1, MAC, createMacToPlayerMap(MAC_2, NAME_2)),
        is(NAME_1));
  }

  @Test
  void assignNameWithMatchingNames() {
    assertThat(
        "name match, should be assigned a numeral name",
        PlayerNameAssigner.assignName(NAME_1, MAC, createMacToPlayerMap(MAC_2, NAME_1)),
        is(NAME_1 + " (1)"));

    assertThat(
        "name match, matching against multiple nodes",
        PlayerNameAssigner.assignName(
            NAME_1, MAC, createMacToPlayerMap(MAC_2, NAME_2, "other-mac", NAME_1)),
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
        PlayerNameAssigner.assignName(
            NAME_1, MAC, createMacToPlayerMap(MAC_2, NAME_1, MAC_2, NAME_1 + " (1)")),
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
        PlayerNameAssigner.assignName(NAME_1, MAC, createMacToPlayerMap(MAC_2, NAME_1 + " (1)")),
        is(NAME_1));

    assertThat(
        "name matches and there is gap in numbering",
        PlayerNameAssigner.assignName(
            NAME_1, MAC, createMacToPlayerMap(MAC_2, NAME_1, MAC_2, NAME_1 + " (2)")),
        is(NAME_1 + " (1)"));

    assertThat(
        "name matches and there is gap in numbering, ordering should not matter",
        PlayerNameAssigner.assignName(
            NAME_1,
            MAC,
            createMacToPlayerMap(
                MAC_2, NAME_1 + " (3)",
                MAC_2, NAME_1 + " (1)",
                MAC_2, NAME_1)),
        is(NAME_1 + " (2)"));

    assertThat(
        "should get next ascending numeral",
        PlayerNameAssigner.assignName(
            NAME_1,
            MAC,
            createMacToPlayerMap(MAC_2, NAME_1 + " (2)", MAC_2, NAME_1 + " (1)", MAC_2, NAME_1)),
        is(NAME_1 + " (3)"));
  }

  /**
   * Not only will matching names cause a numeral increment, but if we see the same address then
   * we'll ignore the requested name and append a numeral increment.
   */
  @Test
  void matchingAddressWillIncrementOriginalName() {
    assertThat(
        "addresses match",
        PlayerNameAssigner.assignName(NAME_1, MAC, createMacToPlayerMap(MAC, NAME_2)),
        is(NAME_2 + " (1)"));

    assertThat(
        "addresses match, ordering should not matter",
        PlayerNameAssigner.assignName(
            NAME_1, MAC, createMacToPlayerMap(MAC, NAME_2 + " (1)", MAC, NAME_2)),
        is(NAME_2 + " (2)"));

    assertThat(
        "addresses match, should fill in gaps in the numerals",
        PlayerNameAssigner.assignName(
            NAME_1, MAC, createMacToPlayerMap(MAC, NAME_2 + " (2)", MAC, NAME_2)),
        is(NAME_2 + " (1)"));
  }

  private static Multimap<String, String> createMacToPlayerMap(
      final String mac1, final String name1) {
    final Multimap<String, String> map = HashMultimap.create();
    map.put(mac1, name1);
    return map;
  }

  private static Multimap<String, String> createMacToPlayerMap(
      final String mac1, final String name1, final String mac2, final String name2) {
    final Multimap<String, String> map = HashMultimap.create();
    map.put(mac1, name1);
    map.put(mac2, name2);
    return map;
  }

  private static Multimap<String, String> createMacToPlayerMap(
      final String mac1,
      final String name1,
      final String mac2,
      final String name2,
      final String mac3,
      final String name3) {
    final Multimap<String, String> map = HashMultimap.create();
    map.put(mac1, name1);
    map.put(mac2, name2);
    map.put(mac3, name3);
    return map;
  }
}
