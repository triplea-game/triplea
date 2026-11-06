package games.strategy.engine.framework.map.download;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.io.Serial;
import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The map's preview at the top of the details, with its lower border as a handle: a click slides
 * the picture up out of the way, so the description gets the room, another slides it back down. The
 * next map shows the same way.
 */
final class PreviewSlider extends JPanel {
  @Serial private static final long serialVersionUID = 4120851317339240951L;
  private static final int HANDLE_HEIGHT = 18;
  private static final int STEP_MILLIS = 15;

  /** The share of the picture a step of the slide moves, so it takes about a tenth of a second. */
  private static final double STEP = 0.15;

  private final Handle handle = new Handle();
  private final Timer slide = new Timer(STEP_MILLIS, e -> step());
  private @Nullable MapPreviewBox preview;
  private boolean open = true;

  /** How much of the picture shows, from 0 (slid up) to 1 (all of it). */
  private double shown = 1;

  PreviewSlider() {
    super(null);
    setOpaque(false);
    handle.addActionListener(e -> toggle());
    handle.describe(open);
    add(handle);
  }

  /** Shows the preview of another map, open or slid up as the one before. */
  void showPreview(final MapPreviewBox newPreview) {
    if (preview != null) {
      remove(preview);
    }
    preview = newPreview;
    add(newPreview);
    revalidate();
    repaint();
  }

  @VisibleForTesting
  void toggle() {
    open = !open;
    handle.describe(open);
    slide.start();
  }

  @VisibleForTesting
  boolean isOpen() {
    return open;
  }

  /** One step of the slide, as the timer runs it; the last one stops the timer. */
  @VisibleForTesting
  void step() {
    final double target = open ? 1 : 0;
    shown = open ? Math.min(target, shown + STEP) : Math.max(target, shown - STEP);
    if (shown == target) {
      slide.stop();
    }
    revalidate();
    repaint();
  }

  private int pictureHeight() {
    return preview == null ? 0 : preview.getPreferredSize().height;
  }

  private int shownHeight() {
    return (int) Math.round(pictureHeight() * shown);
  }

  @Override
  public void doLayout() {
    if (preview != null) {
      // the picture moves up behind the top edge, its lower part last
      preview.setBounds(0, shownHeight() - pictureHeight(), getWidth(), pictureHeight());
    }
    handle.setBounds(0, shownHeight(), getWidth(), HANDLE_HEIGHT);
  }

  @Override
  public Dimension getPreferredSize() {
    final int width = preview == null ? 0 : preview.getPreferredSize().width;
    return new Dimension(width, shownHeight() + HANDLE_HEIGHT);
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  /** The lower border of the picture: a hairline with a small arrow up or down in its middle. */
  private static final class Handle extends JButton {
    @Serial private static final long serialVersionUID = -6911604513574214003L;
    private static final int ARROW = 5;

    private boolean pointsUp = true;

    Handle() {
      setContentAreaFilled(false);
      setBorderPainted(false);
      setFocusPainted(false);
      setOpaque(false);
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void describe(final boolean pictureShown) {
      pointsUp = pictureShown;
      final String text = pictureShown ? "Hide picture" : "Show picture";
      setToolTipText(text);
      getAccessibleContext().setAccessibleName(text);
      repaint();
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final int middle = getHeight() / 2;
      final int centre = getWidth() / 2;
      final boolean lit = getModel().isRollover() || isFocusOwner();
      g2.setColor(lit ? LookAndFeel.getAccentColor() : LookAndFeel.getSeparatorColor());
      // the arrow sits in a gap of the line, so it reads as a handle
      final int gap = 3 * ARROW;
      g2.fillRect(0, middle, centre - gap, 1);
      g2.fillRect(centre + gap, middle, getWidth() - centre - gap, 1);
      g2.setColor(lit ? LookAndFeel.getAccentColor() : LookAndFeel.getSecondaryTextColor());
      g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      final int tip = pointsUp ? middle - ARROW / 2 - 1 : middle + ARROW / 2 + 1;
      final int base = pointsUp ? tip + ARROW : tip - ARROW;
      final Path2D arrow = new Path2D.Float();
      arrow.moveTo(centre - ARROW * 1.4, base);
      arrow.lineTo(centre, tip);
      arrow.lineTo(centre + ARROW * 1.4, base);
      g2.draw(arrow);
      g2.dispose();
    }
  }
}
