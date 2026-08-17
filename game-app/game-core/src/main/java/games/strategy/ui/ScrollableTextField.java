package games.strategy.ui;

import games.strategy.engine.framework.system.SystemProperties;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;
import org.triplea.swing.IntTextField;
import org.triplea.swing.IntTextFieldChangeListener;

/**
 * A UI component that displays a scrollable text field for inputting integers. Four buttons are
 * provided to change the text field value: set maximum value, increment value, decrement value, and
 * set minimum value.
 */
public class ScrollableTextField extends JPanel {
  private static final long serialVersionUID = 6940592988573672224L;

  private static final Icon UP_ICON = new StepperIcon(StepperKind.UP);
  private static final Icon DOWN_ICON = new StepperIcon(StepperKind.DOWN);
  private static final Icon MAX_ICON = new StepperIcon(StepperKind.MAX);
  private static final Icon MIN_ICON = new StepperIcon(StepperKind.MIN);

  private final IntTextField text;
  private final JButton upButton;
  private final JButton downButton;
  private final JButton maxButton;
  private final JButton minButton;
  private final List<ScrollableTextFieldListener> listeners = new ArrayList<>();

  public ScrollableTextField(final int minVal, final int maxVal) {
    text = new IntTextField(minVal, maxVal);
    setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    add(text);
    Insets inset = new Insets(0, 0, 0, 0);
    if (SystemProperties.isMac()) {
      inset = new Insets(2, 0, 2, 0);
    }
    upButton = new JButton(UP_ICON);
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
    downButton = new JButton(DOWN_ICON);
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
    maxButton = new JButton(MAX_ICON);
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
    minButton = new JButton(MIN_ICON);
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

  /**
   * FlatLaf's default {@code Button.minimumWidth} is 72px and its disabled-icon filter turns the
   * old black GIF arrows invisible on dark themes. Keep these steppers compact and let the icon
   * paint with the button foreground.
   */
  private static void configureStepperButton(final JButton button, final Insets inset) {
    button.setMargin(inset);
    button.putClientProperty("JButton.buttonType", "toolBarButton");
    button.putClientProperty("JComponent.minimumWidth", 0);
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

  private enum StepperKind {
    UP,
    DOWN,
    MAX,
    MIN
  }

  /** Paints with the button's current foreground so disabled arrows stay visible on FlatLaf. */
  private static final class StepperIcon implements Icon {
    private static final int WIDTH = 12;
    private static final int HEIGHT = 8;
    private final StepperKind kind;

    StepperIcon(final StepperKind kind) {
      this.kind = kind;
    }

    @Override
    public int getIconWidth() {
      return WIDTH;
    }

    @Override
    public int getIconHeight() {
      return HEIGHT;
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      final Color color =
          c.isEnabled()
              ? c.getForeground()
              : Optional.ofNullable(UIManager.getColor("Button.disabledText"))
                  .orElse(c.getForeground());
      final Graphics2D g2 = (Graphics2D) g.create();
      g2.setColor(color);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      switch (kind) {
        case UP -> fillTriangle(g2, x, y, HEIGHT, true);
        case DOWN -> fillTriangle(g2, x, y, HEIGHT, false);
        case MAX -> {
          g2.fillRect(x + 1, y, WIDTH - 2, 2);
          fillTriangle(g2, x, y + 2, HEIGHT - 2, true);
        }
        case MIN -> {
          fillTriangle(g2, x, y, HEIGHT - 2, false);
          g2.fillRect(x + 1, y + HEIGHT - 2, WIDTH - 2, 2);
        }
      }
      g2.dispose();
    }

    private static void fillTriangle(
        final Graphics2D g2, final int x, final int y, final int height, final boolean up) {
      final Polygon triangle = new Polygon();
      if (up) {
        triangle.addPoint(x + WIDTH / 2, y);
        triangle.addPoint(x + 1, y + height - 1);
        triangle.addPoint(x + WIDTH - 1, y + height - 1);
      } else {
        triangle.addPoint(x + 1, y);
        triangle.addPoint(x + WIDTH - 1, y);
        triangle.addPoint(x + WIDTH / 2, y + height - 1);
      }
      g2.fillPolygon(triangle);
    }
  }
}
