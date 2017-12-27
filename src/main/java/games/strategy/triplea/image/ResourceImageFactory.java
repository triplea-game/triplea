package games.strategy.triplea.image;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import games.strategy.engine.data.Resource;
import games.strategy.triplea.ResourceLoader;
import games.strategy.ui.Util;

public class ResourceImageFactory {
  public static final int DEFAULT_RESOURCE_ICON_SIZE = 12;
  public static final int LARGE_RESOURCE_ICON_SIZE = 24;
  private static final String FILE_NAME_BASE = "resources/";
  // maps Point -> Icon
  private final Map<String, ImageIcon> icons = new HashMap<>();
  // Scaling factor for images
  private double scaleFactor;
  private ResourceLoader resourceLoader;

  /** Creates new ResourceImageFactory. */
  public ResourceImageFactory() {}

  public void setResourceLoader(final ResourceLoader loader, final double scaleFactor) {
    this.scaleFactor = scaleFactor;
    resourceLoader = loader;
    clearImageCache();
  }

  /**
   * Set the scaling factor.
   */
  public void setScaleFactor(final double scaleFactor) {
    if (this.scaleFactor != scaleFactor) {
      this.scaleFactor = scaleFactor;
      clearImageCache();
    }
  }

  /**
   * Return the scaling factor.
   */
  public double getScaleFactor() {
    return scaleFactor;
  }

  /**
   * Return the width of scaled.
   */
  public int getUnitImageWidth(final boolean large) {
    return (int) (scaleFactor * (large ? LARGE_RESOURCE_ICON_SIZE : DEFAULT_RESOURCE_ICON_SIZE));
  }

  /**
   * Return the height of scaled.
   */
  public int getUnitImageHeight(final boolean large) {
    return (int) (scaleFactor * (large ? LARGE_RESOURCE_ICON_SIZE : DEFAULT_RESOURCE_ICON_SIZE));
  }

  // Clear the image and icon cache
  private void clearImageCache() {
    icons.clear();
  }

  private Image getBaseImage(final String baseImageName) {
    // URL uses '/' not '\'
    final String fileName = FILE_NAME_BASE + baseImageName + ".png";
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
   */
  public ImageIcon getIcon(final Resource type, final boolean large) {
    final String fullName = type.getName() + (large ? "_large" : "");
    if (icons.containsKey(fullName)) {
      return icons.get(fullName);
    }
    final Image img = getBaseImage(fullName);
    final ImageIcon icon = new ImageIcon(img);
    icons.put(fullName, icon);
    return icon;
  }
}
