package games.strategy.triplea.image;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

import games.strategy.debug.ClientLogger;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.BlendComposite.BlendingMode;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.ui.Util;

public final class TileImageFactory {
  private final Object mutex = new Object();
  // one instance in the application
  private static final String SHOW_RELIEF_IMAGES_PREFERENCE = "ShowRelief2";
  private static boolean showReliefImages = true;
  private static final String SHOW_MAP_BLENDS_PREFERENCE = "ShowBlends";
  private static boolean showMapBlends = false;
  private static final String SHOW_MAP_BLEND_MODE = "BlendMode";
  private static String showMapBlendMode = "normal";
  private static final String SHOW_MAP_BLEND_ALPHA = "BlendAlpha";
  private static float showMapBlendAlpha = 1.0f;
  private final Composite composite = AlphaComposite.Src;
  private static final GraphicsConfiguration configuration =
      GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
  private static final Logger logger = Logger.getLogger(TileImageFactory.class.getName());
  private double scale = 1.0;
  // maps image name to ImageRef
  private final Map<String, ImageRef> imageCache = new HashMap<>();
  private ResourceLoader resourceLoader;

  static {
    final Preferences prefs = Preferences.userNodeForPackage(TileImageFactory.class);
    showReliefImages = prefs.getBoolean(SHOW_RELIEF_IMAGES_PREFERENCE, true);
    showMapBlends = prefs.getBoolean(SHOW_MAP_BLENDS_PREFERENCE, false);
    showMapBlendMode = prefs.get(SHOW_MAP_BLEND_MODE, "normal");
    showMapBlendAlpha = prefs.getFloat(SHOW_MAP_BLEND_ALPHA, 1.0f);
  }

  public static boolean getShowReliefImages() {
    return showReliefImages;
  }

  public static boolean getShowMapBlends() {
    return showMapBlends;
  }

  private static String getShowMapBlendMode() {
    return showMapBlendMode.toUpperCase();
  }

  private static float getShowMapBlendAlpha() {
    return showMapBlendAlpha;
  }

  public void setScale(final double newScale) {
    if (newScale > 1) {
      throw new IllegalArgumentException("Wrong scale");
    }
    synchronized (mutex) {
      scale = newScale;
      imageCache.clear();
    }
  }

  public static void setShowReliefImages(final boolean showReliefImages) {
    TileImageFactory.showReliefImages = showReliefImages;
    final Preferences prefs = Preferences.userNodeForPackage(TileImageFactory.class);
    prefs.putBoolean(SHOW_RELIEF_IMAGES_PREFERENCE, TileImageFactory.showReliefImages);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      ClientLogger.logQuietly("Failed to save value: " + showReliefImages, ex);
    }
  }

  public static void setShowMapBlends(final boolean showMapBlends) {
    TileImageFactory.showMapBlends = showMapBlends;
    final Preferences prefs = Preferences.userNodeForPackage(TileImageFactory.class);
    prefs.putBoolean(SHOW_MAP_BLENDS_PREFERENCE, TileImageFactory.showMapBlends);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      ClientLogger.logQuietly("faild to save value: " + showMapBlends, ex);
    }
  }

  public static void setShowMapBlendMode(final String showMapBlendMode) {
    TileImageFactory.showMapBlendMode = showMapBlendMode;
    final Preferences prefs = Preferences.userNodeForPackage(TileImageFactory.class);
    prefs.put(SHOW_MAP_BLEND_MODE, TileImageFactory.showMapBlendMode);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      ClientLogger.logQuietly("faild to save value: " + showMapBlendMode, ex);
    }
  }

  public static void setShowMapBlendAlpha(final float showMapBlendAlpha) {
    TileImageFactory.showMapBlendAlpha = showMapBlendAlpha;
    final Preferences prefs = Preferences.userNodeForPackage(TileImageFactory.class);
    prefs.putFloat(SHOW_MAP_BLEND_ALPHA, TileImageFactory.showMapBlendAlpha);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      ClientLogger.logQuietly("faild to save value: " + showMapBlendAlpha, ex);
    }
  }

  public void setMapDir(final ResourceLoader loader) {
    resourceLoader = loader;
    synchronized (mutex) {
      // we manually want to clear each ref to allow the soft reference to
      // be removed
      imageCache.values().forEach(ImageRef::clear);
      imageCache.clear();
    }
  }

  private Image isImageLoaded(final String fileName) {
    if (imageCache.get(fileName) == null) {
      return null;
    }
    return imageCache.get(fileName).getImage();
  }

  public Image getBaseTile(final int x, final int y) {
    final String fileName = getBaseTileImageName(x, y);
    if (resourceLoader.getResource(fileName) == null) {
      return null;
    }
    return getImage(fileName, false);
  }

  public Image getUnscaledUncachedBaseTile(final int x, final int y) {
    final String fileName = getBaseTileImageName(x, y);
    final URL url = resourceLoader.getResource(fileName);
    if (url == null) {
      return null;
    }
    return loadImage(url, fileName, false, false, false);
  }

  private static String getBaseTileImageName(final int x, final int y) {
    // we are loading with a class loader now, use /
    final String fileName = "baseTiles" + "/" + x + "_" + y + ".png";
    return fileName;
  }

  private Image getImage(final String fileName, final boolean transparent) {
    synchronized (mutex) {
      final Image image = isImageLoaded(fileName);
      if (image != null) {
        return image;
      }
      // This is null if there is no image
      final URL url = resourceLoader.getResource(fileName);

      if ((!showMapBlends || !showReliefImages || !transparent) && (url == null)) {
        return null;
      }
      loadImage(url, fileName, transparent, true, true);
    }
    return getImage(fileName, transparent);
  }

  public Image getReliefTile(final int a, final int b) {
    final String fileName = getReliefTileImageName(a, b);
    return getImage(fileName, true);
  }

  public Image getUnscaledUncachedReliefTile(final int x, final int y) {
    final String fileName = getReliefTileImageName(x, y);
    final URL url = resourceLoader.getResource(fileName);
    if (url == null) {
      return null;
    }
    return loadImage(url, fileName, true, false, false);
  }

  private static String getReliefTileImageName(final int x, final int y) {
    // we are loading with a class loader now, use /
    final String fileName = "reliefTiles" + "/" + x + "_" + y + ".png";
    return fileName;
  }

  /**
   * @return compatibleImage This method produces a blank white tile for use in blending.
   */
  private static BufferedImage makeMissingBaseTile(final BufferedImage input) {
    final BufferedImage compatibleImage =
        configuration.createCompatibleImage(input.getWidth(null), input.getHeight(null), Transparency.TRANSLUCENT);
    final Graphics2D g2 = compatibleImage.createGraphics();
    g2.fillRect(0, 0, input.getWidth(null), input.getHeight(null));
    g2.drawImage(compatibleImage, 0, 0, null);
    g2.dispose();
    return compatibleImage;
  }

  private Image loadImage(final URL imageLocation, final String fileName, final boolean transparent,
      final boolean cache, final boolean scale) {
    return (showMapBlends && showReliefImages && transparent)
        ? loadBlendedImage(fileName, cache, scale)
        : loadUnblendedImage(imageLocation, fileName, transparent, cache, scale);
  }

  private Image loadBlendedImage(final String fileName, final boolean cache, final boolean scaled) {
    BufferedImage reliefFile = null;
    BufferedImage baseFile = null;
    // The relief tile
    final String reliefFileName = fileName.replace("baseTiles", "reliefTiles");
    final URL urlrelief = resourceLoader.getResource(reliefFileName);
    // The base tile
    final String baseFileName = fileName.replace("reliefTiles", "baseTiles");
    final URL urlBase = resourceLoader.getResource(baseFileName);
    // blank relief tile
    final String blankReliefFileName = "reliefTiles/blank_relief.png";
    final URL urlBlankRelief = resourceLoader.getResource(blankReliefFileName);

    // Get buffered images
    try {
      final Stopwatch loadingImages =
          new Stopwatch(logger, Level.FINE, "Loading images:" + urlrelief + " and " + urlBase);
      if (urlrelief != null) {
        reliefFile = loadCompatibleImage(urlrelief);
      }
      if (urlBase != null) {
        baseFile = loadCompatibleImage(urlBase);
      }
      loadingImages.done();
    } catch (final IOException e) {
      ClientLogger.logQuietly("Failed to load one or more images: " + urlrelief + ", " + urlBase, e);
    }

    // This does the blend
    final float alpha = getShowMapBlendAlpha();
    if (reliefFile == null) {
      try {
        reliefFile = loadCompatibleImage(urlBlankRelief);
      } catch (final IOException e) {
        ClientLogger.logQuietly("Failed to load image: " + urlBlankRelief, e);
      }
    }
    // This fixes the blank land territories
    if (baseFile == null) {
      baseFile = makeMissingBaseTile(reliefFile);
    }
    /* reversing the to/from files leaves white underlays visible */
    if (reliefFile != null) {
      final BufferedImage blendedImage =
          new BufferedImage(reliefFile.getWidth(null), reliefFile.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      final Graphics2D g2 = blendedImage.createGraphics();
      if (scaled && (scale != 1.0)) {
        final AffineTransform transform = new AffineTransform();
        transform.scale(scale, scale);
        g2.setTransform(transform);
      }
      g2.drawImage(reliefFile, 0, 0, null);
      final BlendingMode blendMode = BlendComposite.BlendingMode.valueOf(getShowMapBlendMode());
      final BlendComposite blendComposite = BlendComposite.getInstance(blendMode).derive(alpha);
      g2.setComposite(blendComposite);
      g2.drawImage(baseFile, 0, 0, null);
      final ImageRef ref = new ImageRef(blendedImage);
      if (cache) {
        imageCache.put(fileName, ref);
      }
      return blendedImage;
    }

    final ImageRef ref = new ImageRef(baseFile);
    if (cache) {
      imageCache.put(fileName, ref);
    }
    return baseFile;
  }

  private Image loadUnblendedImage(final URL imageLocation, final String fileName, final boolean transparent,
      final boolean cache, final boolean scaled) {
    Image image;
    try {
      final Stopwatch loadingImages = new Stopwatch(logger, Level.FINE, "Loading image:" + imageLocation);
      final BufferedImage fromFile = ImageIO.read(imageLocation);
      loadingImages.done();
      final Stopwatch copyingImage = new Stopwatch(logger, Level.FINE, "Copying image:" + imageLocation);
      // if we dont copy, drawing the tile to the screen takes significantly longer
      // has something to do with the colour model and type of the images
      // some images can be copeid quickly to the screen
      // this step is a significant bottle neck in the image drawing process
      // we should try to find a way to avoid it, and load the
      // png directly as the right type
      image = Util.createImage(fromFile.getWidth(null), fromFile.getHeight(null), transparent);
      final Graphics2D g = (Graphics2D) image.getGraphics();
      if (scaled && (scale != 1.0)) {
        final AffineTransform transform = new AffineTransform();
        transform.scale(scale, scale);
        g.setTransform(transform);
      }
      g.drawImage(fromFile, 0, 0, null);
      g.dispose();
      fromFile.flush();
      copyingImage.done();
    } catch (final IOException e) {
      ClientLogger.logError("Could not load image, url: " + imageLocation.toString(), e);
      image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    }
    final ImageRef ref = new ImageRef(image);
    if (cache) {
      imageCache.put(fileName, ref);
    }
    return image;
  }

  public Composite getComposite() {
    return this.composite;
  }

  private static BufferedImage loadCompatibleImage(final URL resource) throws IOException {
    final BufferedImage image = ImageIO.read(resource);
    return toCompatibleImage(image);
  }

  private static BufferedImage toCompatibleImage(final BufferedImage image) {
    final BufferedImage compatibleImage =
        configuration.createCompatibleImage(image.getWidth(), image.getHeight(), Transparency.TRANSLUCENT);
    final Graphics g = compatibleImage.getGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return compatibleImage;
  }
}
