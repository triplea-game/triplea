package games.strategy.triplea.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;

/** Unit tests for the map skin preference written from the View menu. */
final class UiContextTest {
  private static final String MAP_SKIN = "MapSkin";

  @Test
  @DisplayName("A chosen skin name is stored under MapSkin")
  void shouldStoreSkinName() {
    final Preferences prefs = new MemoryPreferences();

    UiContext.setMapSkinPreference(prefs, "NATO");

    assertThat(prefs.get(MAP_SKIN, null))
        .as("empty preferences should remember the selected skin")
        .isEqualTo("NATO");
  }

  @Test
  @DisplayName("Original removes a previously stored map skin")
  void shouldRemoveSkinWhenOriginalIsSelected() throws BackingStoreException {
    final Preferences prefs = new MemoryPreferences();
    prefs.put(MAP_SKIN, "NATO");

    UiContext.setMapSkinPreference(prefs, "Original");

    assertOriginalSkinIsNotStored(prefs);
  }

  @Test
  @DisplayName("A new skin name replaces the previously stored skin")
  void shouldReplaceStoredSkin() {
    final Preferences prefs = new MemoryPreferences();
    prefs.put(MAP_SKIN, "NATO");

    UiContext.setMapSkinPreference(prefs, "Winter");

    assertThat(prefs.get(MAP_SKIN, null))
        .as("selecting another skin replaces the saved name")
        .isEqualTo("Winter");
  }

  @Test
  @DisplayName("Original leaves MapSkin absent when nothing was stored")
  void shouldLeaveSkinAbsentWhenOriginalIsSelected() throws BackingStoreException {
    final Preferences prefs = new MemoryPreferences();

    UiContext.setMapSkinPreference(prefs, "Original");

    assertOriginalSkinIsNotStored(prefs);
  }

  @Test
  @DisplayName("Skin names keep the spelling supplied by the menu")
  void shouldKeepSkinNameSpelling() {
    final Preferences prefs = new MemoryPreferences();

    UiContext.setMapSkinPreference(prefs, "Nato");

    assertThat(prefs.get(MAP_SKIN, null))
        .as("the writer stores the menu string without rewriting its spelling")
        .isEqualTo("Nato");
  }

  private static void assertOriginalSkinIsNotStored(final Preferences prefs)
      throws BackingStoreException {
    assertThat(prefs.get(MAP_SKIN, null))
        .as("Original clears MapSkin so the map art is used")
        .isNull();
    assertThat(Arrays.asList(prefs.keys()))
        .as("Original is not stored as a preference key")
        .doesNotContain(MAP_SKIN, "Original");
    assertThat(Arrays.stream(prefs.keys()).map(key -> prefs.get(key, null)))
        .as("the node does not contain the literal Original")
        .doesNotContain("Original");
  }
}
