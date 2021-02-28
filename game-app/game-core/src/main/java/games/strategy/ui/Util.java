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
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import lombok.experimental.UtilityClass;

/** A collection of methods useful for rendering the UI. */
@UtilityClass
public final class Util {
  public static final String TERRITORY_SEA_ZONE_INFIX = "Sea Zone";

  private static final Component component =
      new Component() {
        private static final long serialVersionUID = 1800075529163275600L;
      };

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

  /**
   * Returns the size of the screen associated with the passed window. Note: The returned size is in
   * logical pixels, rather than screen pixels - which is the same unit Swing UI sizes are in.
   */
  public static Dimension getScreenSize(final Window window) {
    final var graphicsConfiguration = window.getGraphicsConfiguration();
    if (graphicsConfiguration == null) {
      return Toolkit.getDefaultToolkit().getScreenSize();
    }
    // Note: The bounds of the graphic config is in logical pixels, which
    // is what we want, unlike gc.getDevice().getDisplayMode().getWidth().
    return graphicsConfiguration.getBounds().getSize();
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
