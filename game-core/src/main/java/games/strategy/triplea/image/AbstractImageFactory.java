package games.strategy.triplea.image;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import games.strategy.engine.data.NamedAttachable;
import games.strategy.triplea.ResourceLoader;
import games.strategy.ui.Util;

/**
 * Contains common methods for image factories.
 */
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
    final String fileName = getFileNameBase() + baseImageName + ".png";
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
  public ImageIcon getIcon(final NamedAttachable type, final boolean large) {
    final String fullName = type.getName() + (large ? "_large" : "");
    if (icons.containsKey(fullName)) {
      return icons.get(fullName);
    }
    // we draw the icon size to fit in the lower window bar
    final int imageDimension = 32;
    final Image img = getScaledImage(getBaseImage(fullName), imageDimension, imageDimension);
    final ImageIcon icon = new ImageIcon(img);
    icons.put(fullName, icon);
    return icon;
  }

  private static Image getScaledImage(final Image srcImg, final int width, final int height) {
    final BufferedImage resizedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2 = resizedImg.createGraphics();

    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2.drawImage(srcImg, 0, 0, width, height, null);
    g2.dispose();

    return resizedImg;
  }

}
