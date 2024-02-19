package games.strategy.triplea.image;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.UnitIconProperties;
import java.awt.Image;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;

/** Retrieves and caches unit icon images. */
public class UnitIconImageFactory extends ImageFactory {
  private static final String PREFIX = "unitIcons/";

  private final GameData data;
  @Getter private final UnitIconProperties unitIconProperties;

  public UnitIconImageFactory(GameData data, ResourceLoader loader) {
    this.data = data;
    unitIconProperties = new UnitIconProperties(data, loader);
    setResourceLoader(loader);
  }

  public List<Image> getImages(String player, String unitType) {
    return unitIconProperties.getImagePaths(player, unitType, data).stream()
        .map(imagePath -> getImage(PREFIX + imagePath).orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
