package games.strategy.triplea.image;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import games.strategy.triplea.ResourceLoader;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

/**
 * Superclass for all image factories.
 *
 * <p>Instances of this class are not thread safe, and its methods are intended to be called from
 * the EDT.
 */
public class ImageFactory {
  private ResourceLoader resourceLoader;
  private final LoadingCache<String, Optional<Image>> cache =
      CacheBuilder.newBuilder()
          .softValues()
          .build(
              new CacheLoader<>() {
                @Override
                public @Nonnull Optional<Image> load(@Nonnull String key) throws IOException {
                  URL url =
                      resourceLoader.getResource(key, ResourceLoader.ASSETS_FOLDER + '/' + key);
                  if (url == null) {
                    return Optional.empty();
                  }
                  ImageIO.setUseCache(false); // refers whether to "use a disk based cache"
                  return Optional.of(ImageIO.read(url));
                }
              });

  public void setResourceLoader(final ResourceLoader loader) {
    resourceLoader = loader;
    cache.invalidateAll();
  }

  /**
   * Returns an image provide an 'image key'. Additional keys can be provided as fallback values.
   *
   * @throws IllegalStateException thrown if none of the image keys can be found
   */
  protected Image getImageOrThrow(final String... keys) {
    return getImage(keys)
        .orElseThrow(() -> new IllegalStateException("Image Not Found: " + keys[0]));
  }

  /**
   * Returns an image provided an 'image key'. Additional keys can be provided as fallback values.
   *
   * @return An empty optional if no image can be found under any key, otherwise a loaded image is
   *     returned.
   */
  protected Optional<Image> getImage(final String... keys) {
    return Arrays.stream(keys)
        .map(cache::getUnchecked)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }
}
