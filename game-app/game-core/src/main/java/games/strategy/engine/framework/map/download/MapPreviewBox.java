package games.strategy.engine.framework.map.download;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.io.Serial;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 * The preview picture of a map with rounded corners; a neutral box while it loads or if there is
 * none. It is loaded when it is first painted, so a long list loads only the previews the player
 * sees. While the map downloads, a thin bar across the bottom of the picture shows how far it got.
 * The picture fills the box, which has the size of its previews unless it is resized, eg to the
 * width of the details.
 */
final class MapPreviewBox extends JComponent {
  @Serial private static final long serialVersionUID = -2410957263446307766L;
  private static final int ARC = 12;
  private static final int BAR_HEIGHT = 5;
  private static final Color BAR_TRACK = new Color(0, 0, 0, 140);

  private final transient MapPreviews previews;
  private final String url;
  private transient @Nullable ImageIcon icon;
  private boolean loadRequested;
  // the percent of the download, or -1 while the map does not download
  private int downloadPercent = -1;

  MapPreviewBox(final MapPreviews previews, final String url) {
    this.previews = previews;
    this.url = url;
    icon = previews.get(url).orElse(null);
    resize(previews.getSize().getWidth());
  }

  /** Gives the box the width, and the height that keeps the shape of its previews. */
  void resize(final int width) {
    final Dimension size =
        new Dimension(
            width, width * previews.getSize().getHeight() / previews.getSize().getWidth());
    setPreferredSize(size);
    setMinimumSize(size);
    setMaximumSize(size);
    revalidate();
  }

  /** Shows the download bar, filled to the percent from 0 to 100. */
  void showProgress(final int percent) {
    downloadPercent = Math.clamp(percent, 0, 100);
    repaint();
  }

  @Override
  protected void paintComponent(final Graphics g) {
    if (icon == null && !loadRequested && !url.isEmpty()) {
      loadRequested = true;
      previews.load(
          url,
          loaded ->
              loaded.ifPresent(
                  picture -> {
                    icon = picture;
                    repaint();
                  }));
    }
    final int width = getPreferredSize().width;
    final int height = getPreferredSize().height;
    final Graphics2D graphics = (Graphics2D) g.create();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    final int top = Math.max(0, (getHeight() - height) / 2);
    final Shape box = new RoundRectangle2D.Float(0, top, width, height, ARC, ARC);
    graphics.setColor(
        Optional.ofNullable(UIManager.getColor("TextField.background"))
            .orElseGet(this::getBackground));
    graphics.fill(box);
    graphics.clip(box);
    if (icon != null) {
      drawPicture(graphics, icon, new Rectangle(0, top, width, height));
    }
    if (downloadPercent >= 0) {
      final int barTop = top + height - BAR_HEIGHT;
      graphics.setColor(BAR_TRACK);
      graphics.fillRect(0, barTop, width, BAR_HEIGHT);
      graphics.setColor(LookAndFeel.getAccentColor());
      graphics.fillRect(0, barTop, width * downloadPercent / 100, BAR_HEIGHT);
    }
    graphics.dispose();
  }

  /**
   * Draws the picture, which has the screen's pixels, into the box at its layout size, so a scaled
   * screen shows it sharp; a picture smaller than the box stays at its size, centered.
   */
  private void drawPicture(
      final Graphics2D graphics, final ImageIcon picture, final Rectangle box) {
    final Rectangle drawn =
        pictureBounds(
            picture.getIconWidth(), picture.getIconHeight(), previews.getDeviceScale(), box);
    graphics.drawImage(picture.getImage(), drawn.x, drawn.y, drawn.width, drawn.height, null);
  }

  /**
   * Returns where the picture goes in the box: all of it if the picture has enough pixels for the
   * box on the screen (within one layout pixel), else at its own size in the middle, so it is never
   * blown up.
   */
  @VisibleForTesting
  static Rectangle pictureBounds(
      final int pixelWidth, final int pixelHeight, final double deviceScale, final Rectangle box) {
    final int width = (int) Math.round(pixelWidth / deviceScale);
    final int height = (int) Math.round(pixelHeight / deviceScale);
    if (width >= box.width - 1) {
      return box;
    }
    return new Rectangle(
        box.x + (box.width - width) / 2, box.y + (box.height - height) / 2, width, height);
  }
}
