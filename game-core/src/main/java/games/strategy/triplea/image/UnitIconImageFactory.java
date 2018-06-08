package games.strategy.triplea.image;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.UnitIconProperties;

/**
 * Retrieves and caches unit icon images.
 */
public class UnitIconImageFactory extends ImageFactory {
  private static final String PREFIX = "unitIcons/";

  /** Creates new FlagIconImageFactory. */
  public UnitIconImageFactory() {}

  public List<Image> getImages(final String player, final String unitType, final GameData data) {
    final List<Image> images = new ArrayList<>();
    final List<String> imagePaths = UnitIconProperties.getInstance(data).getImagePaths(player, unitType, data);
    for (final String imagePath : imagePaths) {
      final Image image = getImage(PREFIX + imagePath, false);
      if (image != null) {
        images.add(image);
      }
    }
    return images;
  }

}
