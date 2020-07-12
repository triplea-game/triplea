package games.strategy.triplea.image;

import games.strategy.engine.data.NamedAttachable;
import games.strategy.triplea.ResourceLoader;
import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;

/** Contains common methods for image factories. */
public abstract class AbstractImageFactory {

  private final Map<String, Image> images = new HashMap<>();
  private ResourceLoader resourceLoader;

  public void setResourceLoader(final ResourceLoader loader) {
    resourceLoader = loader;
    images.clear();
  }

  protected abstract String getFileNameBase();

  /**
   * Return a icon image.
   *
   * @throws IllegalStateException if image can't be found
   */
  public ImageIcon getIcon(final NamedAttachable type, final boolean large) {
    final String fullName = type.getName() + (large ? "_large" : "");
    return new ImageIcon(images.computeIfAbsent(fullName, this::getBaseImage));
  }

  private Image getBaseImage(final String baseImageName) {
    final String fileName = getFileNameBase() + baseImageName + ".png";
    final URL url = resourceLoader.getResource(fileName);
    if (url == null) {
      throw new IllegalStateException(
          "Can not load image: " + baseImageName + "  looking in: " + fileName);
    }
    return ImageLoader.getImage(url);
  }
}
