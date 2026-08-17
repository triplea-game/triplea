package games.strategy.ui;

import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.EngineImageLoader;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.triplea.swing.IntTextField;
import org.triplea.swing.IntTextFieldChangeListener;

/**
 * A UI component that displays a scrollable text field for inputting integers. Four buttons are
 * provided to change the text field value: set maximum value, increment value, decrement value, and
 * set minimum value.
 *
 * <p>Fixes: #14841
 */
public class ScrollableTextField extends JPanel {
  private static final long serialVersionUID = 6940592988573672224L;

  private static boolean imagesLoaded;
  private static Icon up;
  private static Icon down;
  private static Icon max;
  private static Icon min;

  // Pre-rendered disabled variants cached once at load time.
  private static Icon upDisabled;
  private static Icon downDisabled;
  private static Icon maxDisabled;
  private static Icon minDisabled;

  private final IntTextField text;
  private final JButton upButton;
  private final JButton downButton;
  private final JButton maxButton;
  private final JButton minButton;
  private final List<ScrollableTextFieldListener> listeners = new ArrayList<>();

  public ScrollableTextField(final int minVal, final int maxVal) {
    loadImages();
    text = new IntTextField(minVal, maxVal);
    setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    add(text);
    Insets inset = new Insets(0, 0, 0, 0);
    if (SystemProperties.isMac()) {
      inset = new Insets(2, 0, 2, 0);
    }
    upButton = new JButton(up);
    // use our pre-rendered disabled icon so the arrows remain visible on themes/implementations
    // where Look & Feel disabled-image generation can fail.
    upButton.setDisabledIcon(upDisabled);
    final Action incrementAction =
        new AbstractAction("inc") {
          private static final long serialVersionUID = 2125871167112459475L;

          @Override
          public void actionPerformed(final ActionEvent e) {
            if (text.isEnabled()) {
              text.setValue(text.getValue() + 1);
              setWidgetActivation();
            }
          }
        };
    upButton.addActionListener(incrementAction);
    upButton.setMargin(inset);
    downButton = new JButton(down);
    downButton.setDisabledIcon(downDisabled);
    downButton.setMargin(inset);
    final Action decrementAction =
        new AbstractAction("dec") {
          private static final long serialVersionUID = 787758939168986726L;

          @Override
          public void actionPerformed(final ActionEvent e) {
            if (text.isEnabled()) {
              text.setValue(text.getValue() - 1);
              setWidgetActivation();
            }
          }
        };
    downButton.addActionListener(decrementAction);
    maxButton = new JButton(max);
    maxButton.setDisabledIcon(maxDisabled);
    maxButton.setMargin(inset);
    final Action maxAction =
        new AbstractAction("max") {
          private static final long serialVersionUID = -3899827439573519512L;

          @Override
          public void actionPerformed(final ActionEvent e) {
            if (text.isEnabled()) {
              text.setValue(text.getMax());
              setWidgetActivation();
            }
          }
        };
    maxButton.addActionListener(maxAction);
    minButton = new JButton(min);
    minButton.setDisabledIcon(minDisabled);
    minButton.setMargin(inset);
    final Action minAction =
        new AbstractAction("min") {
          private static final long serialVersionUID = 5785321239855254848L;

          @Override
          public void actionPerformed(final ActionEvent e) {
            if (text.isEnabled()) {
              text.setValue(text.getMin());
              setWidgetActivation();
            }
          }
        };
    minButton.addActionListener(minAction);
    final JPanel upDown = new JPanel();
    upDown.setLayout(new BoxLayout(upDown, BoxLayout.Y_AXIS));
    upDown.add(upButton);
    upDown.add(downButton);
    final JPanel maxMin = new JPanel();
    maxMin.setLayout(new BoxLayout(maxMin, BoxLayout.Y_AXIS));
    maxMin.add(maxButton);
    maxMin.add(minButton);
    add(upDown);
    add(maxMin);
    final IntTextFieldChangeListener textListener = field -> notifyListeners();
    text.addChangeListener(textListener);
    setWidgetActivation();
  }

  private static synchronized void loadImages() {
    if (imagesLoaded) {
      return;
    }
    up = new ImageIcon(EngineImageLoader.loadImage("images", "up.gif"));
    down = new ImageIcon(EngineImageLoader.loadImage("images", "down.gif"));
    max = new ImageIcon(EngineImageLoader.loadImage("images", "max.gif"));
    min = new ImageIcon(EngineImageLoader.loadImage("images", "min.gif"));

    // Create deterministic disabled variants using a simple synchronous per-pixel transform.
    // This preserves the visual mapping used in PR 14876 but avoids Toolkit/FilteredImageSource
    // and MediaTracker complexity.
    upDisabled = createDisabledIcon(up);
    downDisabled = createDisabledIcon(down);
    maxDisabled = createDisabledIcon(max);
    minDisabled = createDisabledIcon(min);

    imagesLoaded = true;
  }

  /**
   * Builds the disabled variant of the given icon by remapping its known enabled colors to known
   * disabled colors. This is a simplified, deterministic per-pixel conversion that preserves the
   * projection + interpolation logic from the prior implementation but is synchronous and easier to
   * review.
   */
  private static Icon createDisabledIcon(final Icon icon) {
    final int width = icon.getIconWidth();
    final int height = icon.getIconHeight();
    if (width <= 0 || height <= 0) {
      return icon;
    }

    final BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final java.awt.Graphics2D g = src.createGraphics();
    try {
      icon.paintIcon(null, g, 0, 0);
    } finally {
      g.dispose();
    }

    // Anchor colors measured from the original UI rendering in the previous PR:
    final int inkEnabled = 0x000000;
    final int fillEnabled = 0x55585A;
    final int inkDisabled = 0x585858;
    final int fillDisabled = 0x3C3F41;

    final int ar = (inkEnabled >>> 16) & 0xff;
    final int ag = (inkEnabled >>> 8) & 0xff;
    final int ab = inkEnabled & 0xff;
    final int fr = (fillEnabled >>> 16) & 0xff;
    final int fg = (fillEnabled >>> 8) & 0xff;
    final int fb = fillEnabled & 0xff;

    final int dr = fr - ar;
    final int dg = fg - ag;
    final int db = fb - ab;
    final double lengthSq = (double) (dr * dr + dg * dg + db * db);
    if (lengthSq == 0.0) {
      // degenerate anchors; nothing to do
      return icon;
    }

    final double toleranceSq = 30.0 * 30.0; // allow modest rounding/compression noise

    final int irr = (inkDisabled >>> 16) & 0xff;
    final int irg = (inkDisabled >>> 8) & 0xff;
    final int irb = inkDisabled & 0xff;
    final int frr = (fillDisabled >>> 16) & 0xff;
    final int frg = (fillDisabled >>> 8) & 0xff;
    final int frb = fillDisabled & 0xff;

    final BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        final int argb = src.getRGB(x, y);
        final int a = (argb >>> 24) & 0xff;
        if (a == 0) {
          dst.setRGB(x, y, argb);
          continue;
        }

        final int r = (argb >>> 16) & 0xff;
        final int gch = (argb >>> 8) & 0xff;
        final int b = argb & 0xff;

        // project this color onto the anchor line between inkEnabled and fillEnabled
        final double tRaw = ((r - ar) * dr + (gch - ag) * dg + (b - ab) * db) / lengthSq;
        final double t = Math.max(0.0, Math.min(1.0, tRaw));

        final double projR = ar + t * dr;
        final double projG = ag + t * dg;
        final double projB = ab + t * db;
        final double distSq =
            (r - projR) * (r - projR) + (gch - projG) * (gch - projG) + (b - projB) * (b - projB);

        if (distSq > toleranceSq) {
          // not on the expected ink<->fill gradient: preserve original pixel
          dst.setRGB(x, y, argb);
          continue;
        }

        final int newR = (int) Math.round(irr + (frr - irr) * t);
        final int newG = (int) Math.round(irg + (frg - irg) * t);
        final int newB = (int) Math.round(irb + (frb - irb) * t);

        final int out = (a << 24) | (newR << 16) | (newG << 8) | newB;
        dst.setRGB(x, y, out);
      }
    }

    return new ImageIcon(dst);
  }

  public void setMax(final int max) {
    text.setMax(max);
    setWidgetActivation();
  }

  public void setShowMaxAndMin(final boolean showMaxAndMin) {
    maxButton.setVisible(showMaxAndMin);
    minButton.setVisible(showMaxAndMin);
  }

  public int getMax() {
    return text.getMax();
  }

  public void setMin(final int min) {
    text.setMin(min);
    setWidgetActivation();
  }

  private void setWidgetActivation() {
    if (text.isEnabled()) {
      final int value = text.getValue();
      final int max = text.getMax();
      final boolean enableUp = (value != max);
      upButton.setEnabled(enableUp);
      maxButton.setEnabled(enableUp);
      final int min = text.getMin();
      final boolean enableDown = (value != min);
      downButton.setEnabled(enableDown);
      minButton.setEnabled(enableDown);
    } else {
      upButton.setEnabled(false);
      downButton.setEnabled(false);
      maxButton.setEnabled(false);
      minButton.setEnabled(false);
    }
    invalidate();
  }

  public int getValue() {
    return text.getValue();
  }

  public void setValue(final int value) {
    text.setValue(value);
    setWidgetActivation();
  }

  public void addChangeListener(final ScrollableTextFieldListener listener) {
    listeners.add(listener);
  }

  private void notifyListeners() {
    for (final ScrollableTextFieldListener listener : listeners) {
      listener.changedValue(this);
    }
  }

  @Override
  public void setEnabled(final boolean enabled) {
    text.setEnabled(enabled);
    setWidgetActivation();
  }
}
