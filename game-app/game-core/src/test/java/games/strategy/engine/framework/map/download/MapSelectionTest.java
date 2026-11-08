package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class MapSelectionTest {
  private static final List<String> SHOWN = List.of("Africa", "Europe", "Pacific", "World");

  @DisplayName("A click selects only the clicked map")
  @Test
  void clickSelectsOne() {
    final MapSelection selection = givenSelection("Africa", "Europe");

    selection.select("Pacific");

    assertThat(selection.getSelected()).containsExactly("Pacific");
    assertThat(selection.getLead()).contains("Pacific");
  }

  @DisplayName("A ctrl-click adds a map, a second one removes it again")
  @Test
  void ctrlClickToggles() {
    final MapSelection selection = givenSelection("Africa");

    selection.toggle("Pacific");
    final List<String> added = selection.getSelected();
    selection.toggle("Pacific");

    assertThat(List.of(added, selection.getSelected()))
        .as("added, then removed; the details show the other map again")
        .containsExactly(List.of("Africa", "Pacific"), List.of("Africa"));
    assertThat(selection.getLead()).contains("Africa");
  }

  @DisplayName("A ctrl-click on the only selected map keeps it, so the details are never empty")
  @Test
  void ctrlClickKeepsLastMap() {
    final MapSelection selection = givenSelection("Africa");

    selection.toggle("Africa");

    assertThat(selection.getSelected()).containsExactly("Africa");
  }

  @DisplayName("A shift-click selects the shown maps from the last clicked one to the clicked one")
  @Test
  void shiftClickSelectsRange() {
    final MapSelection selection = givenSelection("World");

    selection.selectRange(SHOWN, "Europe");

    assertThat(selection.getSelected())
        .as("upwards from World to Europe, in the order shown")
        .containsExactly("Europe", "Pacific", "World");
    assertThat(selection.getLead()).contains("Europe");
  }

  @DisplayName("Shift-clicks start their range where the last click without shift was")
  @Test
  void shiftClicksKeepAnchor() {
    final MapSelection selection = givenSelection("Europe");
    selection.selectRange(SHOWN, "World");

    selection.selectRange(SHOWN, "Africa");

    assertThat(selection.getSelected())
        .as("the range from Europe now goes the other way")
        .containsExactly("Africa", "Europe");
  }

  @DisplayName("Hiding the lead map, eg by a search, selects the first shown map")
  @Test
  void hiddenLeadPassesOn() {
    final MapSelection selection = givenSelection("Pacific");

    selection.keepShown(List.of("Africa", "Europe"));

    assertThat(selection.getLead()).contains("Africa");
    assertThat(selection.getSelected()).containsExactly("Africa");
  }

  @DisplayName("Without shown maps, nothing is selected")
  @Test
  void nothingShownNothingSelected() {
    final MapSelection selection = givenSelection("Pacific");

    selection.keepShown(List.of());

    assertThat(selection.getLead()).isEmpty();
    assertThat(selection.getSelected()).isEmpty();
  }

  @DisplayName("Clear keeps only the map the details show")
  @Test
  void clearKeepsLead() {
    final MapSelection selection = givenSelection("Africa", "Europe");

    selection.clear();

    assertThat(selection.getSelected()).containsExactly("Europe");
  }

  /** The maps selected by a click on the first and ctrl-clicks on the others. */
  private static MapSelection givenSelection(final String first, final String... others) {
    final MapSelection selection = new MapSelection();
    selection.select(first);
    for (final String other : others) {
      selection.toggle(other);
    }
    return selection;
  }
}
