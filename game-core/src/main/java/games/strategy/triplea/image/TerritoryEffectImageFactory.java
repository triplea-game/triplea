package games.strategy.triplea.image;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import games.strategy.engine.data.TerritoryEffect;
import games.strategy.triplea.ResourceLoader;
import games.strategy.ui.Util;

/**
 * Used to manage territory effect images.
 */
public class TerritoryEffectImageFactory {

  private static final String FILE_NAME_BASE = "territoryEffects/";
  private final Map<String, ImageIcon> icons = new HashMap<>();
  private ResourceLoader resourceLoader;

  public TerritoryEffectImageFactory() {}

  public void setResourceLoader(final ResourceLoader loader) {
    resourceLoader = loader;
    clearImageCache();
  }

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
   *
   * @throws IllegalStateException if image can't be found
   */
  public ImageIcon getIcon(final TerritoryEffect type, final boolean large) {
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
