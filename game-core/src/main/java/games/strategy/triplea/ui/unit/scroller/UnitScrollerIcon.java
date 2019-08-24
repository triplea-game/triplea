package games.strategy.triplea.ui.unit.scroller;

import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.ImageLoader;
import java.io.File;
import java.util.function.Supplier;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/** Class to handle icon paths and getting references to Icon images. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class UnitScrollerIcon implements Supplier<Icon> {

  static final UnitScrollerIcon CENTER_ON_UNIT = new UnitScrollerIcon("unit_center.png");
  static final UnitScrollerIcon UNIT_HIGHLIGHT = new UnitScrollerIcon("lantern.png");
  static final UnitScrollerIcon UNIT_FLAGS = new UnitScrollerIcon("flag.png");

  static final UnitScrollerIcon LEFT_ARROW = new UnitScrollerIcon("left_arrow.png");
  static final UnitScrollerIcon RIGHT_ARROW = new UnitScrollerIcon("right_arrow.png");
  static final UnitScrollerIcon UNIT_SKIP = new UnitScrollerIcon("unit_skip.png");
  static final UnitScrollerIcon UNIT_SLEEP = new UnitScrollerIcon("unit_sleep.png");

  private static final File IMAGE_PATH = new File(ResourceLoader.RESOURCE_FOLDER, "unit_scroller");

  private final String imageFile;

  @Override
  public Icon get() {
    return new ImageIcon(ImageLoader.getImage(new File(IMAGE_PATH, imageFile)));
  }
}
