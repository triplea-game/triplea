package games.strategy.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A collection of methods useful for rendering the UI. */
public final class Util {
  public static final String TERRITORY_SEA_ZONE_INFIX = "Sea Zone";

  private static final Component component =
      new Component() {
        private static final long serialVersionUID = 1800075529163275600L;
      };

  private Util() {}

  public static void ensureImageLoaded(final Image anImage) {
    final MediaTracker tracker = new MediaTracker(component);
    tracker.addImage(anImage, 1);
    try {
      tracker.waitForAll();
      tracker.removeImage(anImage);
    } catch (final InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  public static Image copyImage(final BufferedImage img) {
    final BufferedImage copy = newImage(img.getWidth(), img.getHeight(), false);
    final Graphics2D g = (Graphics2D) copy.getGraphics();
    g.drawImage(img, 0, 0, null);
    g.dispose();
    return copy;
  }

  /**
   * Previously used to use TYPE_INT_BGR and TYPE_INT_ABGR but caused memory problems. Fix is to use
   * 3Byte rather than INT.
   */
  public static BufferedImage newImage(final int width, final int height, final boolean needAlpha) {
    return new BufferedImage(
        width, height, needAlpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);
  }

  /** Centers the specified window on the screen. */
  public static void center(final Window w) {
    final Dimension screenSize = getScreenSize(w);
    final int screenWidth = screenSize.width;
    final int screenHeight = screenSize.height;
    final int windowWidth = w.getWidth();
    final int windowHeight = w.getHeight();
    if (windowHeight > screenHeight) {
      return;
    }
    if (windowWidth > screenWidth) {
      return;
    }
    final int x = (screenWidth - windowWidth) / 2;
    final int y = (screenHeight - windowHeight) / 2;
    w.setLocation(x, y);
  }

  /** Returns the size of the screen associated with the passed window. */
  public static Dimension getScreenSize(final Window window) {
    final var graphicsConfiguration = window.getGraphicsConfiguration();
    if (graphicsConfiguration == null) {
      return Toolkit.getDefaultToolkit().getScreenSize();
    }
    final var displayMode = graphicsConfiguration.getDevice().getDisplayMode();
    return new Dimension(displayMode.getWidth(), displayMode.getHeight());
  }

  /**
   * Creates an image that consists of {@code text} on a background containing a curved shape. The
   * returned image is appropriate for display in the header of a dialog to give it a "wizard-like"
   * look.
   */
  public static Image getBanner(final String text) {
    // code stolen from swingx
    // swingx is lgpl, so no problems with copyright
    final int w = 530;
    final int h = 60;
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g2 = img.createGraphics();
    final Font font = new Font("Arial Bold", Font.PLAIN, 36);
    g2.setFont(font);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(
        RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    // draw a big square
    g2.setColor(Color.GRAY);
    g2.fillRect(0, 0, w, h);
    // create the curve shape
    final GeneralPath curveShape = new GeneralPath(GeneralPath.WIND_NON_ZERO);
    curveShape.moveTo(0, h * .6f);
    curveShape.curveTo(w * .167f, h * 1.2f, w * .667f, h * -.5f, w, h * .75f);
    curveShape.lineTo(w, h);
    curveShape.lineTo(0, h);
    curveShape.lineTo(0, h * .8f);
    curveShape.closePath();
    // draw into the buffer a gradient (bottom to top), and the text "Login"
    final GradientPaint gp = new GradientPaint(0, h, Color.GRAY, 0, 0, Color.LIGHT_GRAY);
    g2.setPaint(gp);
    g2.fill(curveShape);
    // g2.setPaint(Color.white);
    g2.setColor(Color.WHITE);
    final float loginStringY = h * .75f;
    final float loginStringX = w * .05f;
    g2.drawString(text, loginStringX, loginStringY);
    return img;
  }

  /**
   * Finds a land territory name or some sea zone name where the point is contained in according to
   * the territory name -> polygons map.
   *
   * @param p A point on the map.
   * @param terrPolygons a map territory name -> polygons
   */
  public static Optional<String> findTerritoryName(
      final Point p, final Map<String, List<Polygon>> terrPolygons) {
    return Optional.ofNullable(findTerritoryName(p, terrPolygons, null));
  }

  /**
   * Finds a land territory name or some sea zone name where the point is contained in according to
   * the territory name -> polygons map. If no land or sea territory has been found a default name
   * is returned.
   *
   * @param p A point on the map.
   * @param terrPolygons a map territory name -> polygons
   * @param defaultTerrName Default territory name that gets returns if nothing was found.
   * @return found territory name of defaultTerrName
   */
  public static String findTerritoryName(
      final Point p, final Map<String, List<Polygon>> terrPolygons, final String defaultTerrName) {
    String lastWaterTerrName = defaultTerrName;
    // try to find a land territory.
    // sea zones often surround a land territory
    for (final String terrName : terrPolygons.keySet()) {
      final Collection<Polygon> polygons = terrPolygons.get(terrName);
      for (final Polygon poly : polygons) {
        if (poly.contains(p)) {
          if (Util.isTerritoryNameIndicatingWater(terrName)) {
            lastWaterTerrName = terrName;
          } else {
            return terrName;
          }
        } // if p is contained
      } // polygons collection loop
    } // terrPolygons map loop
    return lastWaterTerrName;
  }

  /**
   * Checks whether name indicates water or not (meaning name starts or ends with default text).
   *
   * @param territoryName - territory name
   * @return true if yes, false otherwise
   */
  public static boolean isTerritoryNameIndicatingWater(final String territoryName) {
    return territoryName.endsWith(TERRITORY_SEA_ZONE_INFIX)
        || territoryName.startsWith(TERRITORY_SEA_ZONE_INFIX);
  }

  /**
   * Returns a new polygon that is a copy of {@code polygon} translated by {@code deltaX} along the
   * x-axis and by {@code deltaY} along the y-axis.
   */
  public static Polygon translatePolygon(
      final Polygon polygon, final int deltaX, final int deltaY) {
    checkNotNull(polygon);

    final Polygon translatedPolygon =
        new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
    translatedPolygon.translate(deltaX, deltaY);
    return translatedPolygon;
  }
}
