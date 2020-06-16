package games.strategy.triplea.image;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.UnitIconProperties;
import java.awt.Image;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Retrieves and caches unit icon images. */
public class UnitIconImageFactory extends ImageFactory {
  private static final String PREFIX = "unitIcons/";

  public List<Image> getImages(final String player, final String unitType, final GameData data) {
    return UnitIconProperties.getInstance(data).getImagePaths(player, unitType, data).stream()
        .map(imagePath -> getImage(PREFIX + imagePath, false))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
