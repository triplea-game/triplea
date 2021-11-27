package games.strategy.triplea.image;

import games.strategy.triplea.ResourceLoader;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
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

  protected Image getImage(final String key1, final String key2, final boolean throwIfNotFound) {
    final Image i1 = getImage(key1, false);
    if (i1 != null) {
      return i1;
    }
    return getImage(key2, throwIfNotFound);
  }

  protected Image getImage(final String key, final boolean throwIfNotFound) {
    final URL url = resourceLoader.getResource(key);
    if (url == null) {
      if (throwIfNotFound) {
        throw new IllegalStateException("Image Not Found:" + key);
      }
      return null;
    }
    try {
      // use cache refers whether to "use a disk based cache" or to use a memory based cache
      ImageIO.setUseCache(false);
      return ImageIO.read(url);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
