package games.strategy.engine.framework.map.download;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * The selected maps of a map list, by name, as clicks change them: a click selects one map, a
 * ctrl-click adds or removes one, a shift-click selects the maps from the last clicked one to the
 * clicked one. The lead is the map the details show; one map stays selected while the list shows
 * maps.
 */
final class MapSelection {
  private final Set<String> selected = new LinkedHashSet<>();
  // the map the details show, the last one clicked
  private @Nullable String lead;
  // where a shift-click range starts: the map last clicked without shift
  private @Nullable String anchor;

  /** Selects only the map, eg on a click. */
  void select(final String mapName) {
    selected.clear();
    selected.add(mapName);
    lead = mapName;
    anchor = mapName;
  }

  /** Adds the map to the selection or removes it, eg on a ctrl-click; the last one stays. */
  void toggle(final String mapName) {
    anchor = mapName;
    if (!selected.contains(mapName)) {
      selected.add(mapName);
      lead = mapName;
    } else if (selected.stream().anyMatch(other -> !other.equals(mapName))) {
      // another map stays selected
      selected.remove(mapName);
      if (mapName.equals(lead)) {
        lead = List.copyOf(selected).getLast();
      }
    }
  }

  /**
   * Selects the shown maps from the anchor to the map, eg on a shift-click; without an anchor that
   * is shown, only the map.
   *
   * @param shownMapNames The maps of the list as shown, top to bottom.
   */
  void selectRange(final List<String> shownMapNames, final String mapName) {
    final int from = anchor == null ? -1 : shownMapNames.indexOf(anchor);
    final int to = shownMapNames.indexOf(mapName);
    if (from < 0 || to < 0) {
      select(mapName);
      return;
    }
    selected.clear();
    selected.addAll(shownMapNames.subList(Math.min(from, to), Math.max(from, to) + 1));
    lead = mapName;
  }

  /**
   * Keeps only the shown maps selected, eg after a search; if the lead is not among them, the first
   * shown map is selected instead.
   */
  void keepShown(final List<String> shownMapNames) {
    selected.retainAll(shownMapNames);
    if (lead != null && selected.contains(lead)) {
      return;
    }
    if (shownMapNames.isEmpty()) {
      selected.clear();
      lead = null;
      anchor = null;
    } else {
      select(shownMapNames.getFirst());
    }
  }

  /** Selects only the lead again, eg for Clear. */
  void clear() {
    Optional.ofNullable(lead).ifPresent(this::select);
  }

  /** The map the details show; empty while the list shows no maps. */
  Optional<String> getLead() {
    return Optional.ofNullable(lead);
  }

  boolean isSelected(final String mapName) {
    return selected.contains(mapName);
  }

  /** The selected maps, in the order they were selected. */
  List<String> getSelected() {
    return List.copyOf(selected);
  }
}
