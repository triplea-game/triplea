package games.strategy.triplea.image;

import games.strategy.triplea.ResourceLoader;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.imageio.ImageIO;

/**
 * Superclass for all image factories.
 *
 * <p>Instances of this class are not thread safe, and its methods are intended to be called from
 * the EDT.
 */
public class ImageFactory {
  private ResourceLoader resourceLoader;

  public void setResourceLoader(final ResourceLoader loader) {
    resourceLoader = loader;
  }

  /**
   * Returns an image provide an 'image key'. Additional keys can be provided as fallback values.
   *
   * @throws IllegalStateException thrown if none of the image keys can be found
   */
  protected Image getImageOrThrow(final String key, String... additionalKeys) {
    return getImage(key, additionalKeys)
        .orElseThrow(() -> new IllegalStateException("Image Not Found:" + key));
  }

  /**
   * Returns an image provided an 'image key'. Additional keys can be provided as fallback values.
   *
   * @return An empty optional if no image can be found under any key, otherwise a loaded image is
   *     returned.
   */
  protected Optional<Image> getImage(final String key, String... additionalKeys) {
    List<String> keys = new ArrayList<>(additionalKeys.length + 1);
    keys.add(key);
    keys.addAll(Arrays.asList(additionalKeys));

    return keys.stream() //
        .map(resourceLoader::getResource)
        .filter(Objects::nonNull)
        .findFirst()
        .map(this::loadImageFromUrl);
  }

  private Image loadImageFromUrl(URL url) {
    try {
      // use cache refers whether to "use a disk based cache" or to use a memory based
      // cache
      ImageIO.setUseCache(false);
      return ImageIO.read(url);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
