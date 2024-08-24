package games.strategy.triplea.image;

import games.strategy.triplea.ResourceLoader;
import games.strategy.ui.Util;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import org.jetbrains.annotations.NonNls;

/** Contains common methods for image factories. */
public abstract class AbstractImageFactory {

  private final Map<String, ImageIcon> icons = new HashMap<>();
  private ResourceLoader resourceLoader;

  public void setResourceLoader(final ResourceLoader loader) {
    resourceLoader = loader;
    clearImageCache();
  }

  protected abstract String getFileNameBase();

  private void clearImageCache() {
    icons.clear();
  }

  private Image getBaseImage(final String baseImageName) {
    // URL uses '/' not '\'
    @NonNls final String fileName = getFileNameBase() + baseImageName + ".png";
    final URL url = resourceLoader.getResource(fileName);
    if (url == null) {
      throw new IllegalStateException("Cant load: " + baseImageName + "  looking in: " + fileName);
    }
    final Image image = Toolkit.getDefaultToolkit().getImage(url);
    Util.ensureImageLoaded(image);
    return image;
  }

  /**
   * Return a icon image.
   *
   * @throws IllegalStateException if image can't be found
   */
  public ImageIcon getIcon(final String name) {
    return icons.computeIfAbsent(name, iconName -> new ImageIcon(getBaseImage(iconName)));
  }

  public ImageIcon getLargeIcon(final String name) {
    return getIcon(name + "_large");
  }
}
