package games.strategy.triplea.ui.unit.scroller;

import games.strategy.triplea.ResourceLoader;
import java.awt.Image;
import java.nio.file.Path;
import java.util.function.Supplier;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NonNls;

/** Class to handle icon paths and getting references to Icon images. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class UnitScrollerIcon implements Supplier<Icon> {

  static final UnitScrollerIcon LEFT_ARROW = new UnitScrollerIcon("left_arrow.png");
  static final UnitScrollerIcon RIGHT_ARROW = new UnitScrollerIcon("right_arrow.png");
  static final UnitScrollerIcon SKIP = new UnitScrollerIcon("skip.png");
  static final UnitScrollerIcon WAKE_ALL = new UnitScrollerIcon("wake_all.png");

  @NonNls private static final String UNIT_SCROLLER_IMAGES_FOLDER = "unit_scroller";

  private final String imageFile;

  @Override
  public Icon get() {
    return new ImageIcon(
        ResourceLoader.loadImageAsset(Path.of(UNIT_SCROLLER_IMAGES_FOLDER, imageFile))
            .getScaledInstance(25, 25, Image.SCALE_SMOOTH));
  }
}
