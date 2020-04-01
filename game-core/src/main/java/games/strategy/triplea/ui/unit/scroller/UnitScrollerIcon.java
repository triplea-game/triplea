package games.strategy.triplea.ui.unit.scroller;

import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.ImageLoader;
import java.awt.Image;
import java.io.File;
import java.util.function.Supplier;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/** Class to handle icon paths and getting references to Icon images. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class UnitScrollerIcon implements Supplier<Icon> {

  static final UnitScrollerIcon LEFT_ARROW = new UnitScrollerIcon("left_arrow.png");
  static final UnitScrollerIcon RIGHT_ARROW = new UnitScrollerIcon("right_arrow.png");
  static final UnitScrollerIcon SKIP = new UnitScrollerIcon("skip.png");
  static final UnitScrollerIcon STATION = new UnitScrollerIcon("station.png");
  static final UnitScrollerIcon WAKE_ALL = new UnitScrollerIcon("wake_all.png");

  private static final File IMAGE_PATH = new File(ResourceLoader.RESOURCE_FOLDER, "unit_scroller");

  private final String imageFile;

  @Override
  public Icon get() {
    return new ImageIcon(
        ImageLoader.getImage(new File(IMAGE_PATH, imageFile))
            .getScaledInstance(20, 20, Image.SCALE_SMOOTH));
  }
}
