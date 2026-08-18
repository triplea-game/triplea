package games.strategy.ui;

import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.EngineImageLoader;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.GrayFilter;
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
 */
public class ScrollableTextField extends JPanel {
  private static final long serialVersionUID = 6940592988573672224L;

  private static boolean imagesLoaded;
  private static Icon up;
  private static Icon down;
  private static Icon max;
  private static Icon min;

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
    configureStepperButton(upButton, inset);
    downButton = new JButton(down);
    configureStepperButton(downButton, inset);
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
    configureStepperButton(maxButton, inset);
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
    configureStepperButton(minButton, inset);
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
    imagesLoaded = true;
  }

  /**
   * Keep the original GIF chrome. FlatLaf's disabled-icon filter can turn those black arrows
   * invisible on dark themes, so bake Swing's GrayFilter transform as the disabled icon instead of
   * letting Toolkit/LAF generate an empty one.
   */
  private static void configureStepperButton(final JButton button, final Insets inset) {
    button.setMargin(inset);
    final Icon icon = button.getIcon();
    if (icon instanceof ImageIcon imageIcon) {
      button.setDisabledIcon(grayedIcon(imageIcon));
    }
  }

  /** Same pixel transform as {@link GrayFilter#createDisabledImage}, applied synchronously. */
  private static Icon grayedIcon(final ImageIcon enabled) {
    final int width = Math.max(1, enabled.getIconWidth());
    final int height = Math.max(1, enabled.getIconHeight());
    final BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D graphics = src.createGraphics();
    enabled.paintIcon(null, graphics, 0, 0);
    graphics.dispose();
    final GrayFilter filter = new GrayFilter(true, 50);
    final BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        dst.setRGB(x, y, filter.filterRGB(x, y, src.getRGB(x, y)));
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
