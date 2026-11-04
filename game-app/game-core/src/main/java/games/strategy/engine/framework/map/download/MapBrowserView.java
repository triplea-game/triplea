package games.strategy.engine.framework.map.download;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** How the maps window shows its maps; the view button above its lists chooses one. */
@AllArgsConstructor
@Getter
public enum MapBrowserView {
  /** Compact rows without pictures: name, creator, status and a small button. */
  LIST("List"),
  /** Rows with a preview, a line about the map and its buttons. */
  LIST_WITH_PICTURES("List with pictures"),
  /** A grid of cards, several per row, with a large preview on top. */
  TILES("Tiles");

  /** The name in the menu of the view button. */
  private final String label;
}
