package games.strategy.engine.framework.startup.ui;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.AbstractBorder;
import lombok.experimental.UtilityClass;

/**
 * Draws launch-screen buttons as clickable text that lights up when hovered or focused, the way a
 * game's main menu does, instead of as boxed Swing buttons.
 */
@UtilityClass
public class LaunchScreenStyle {
  /** The red of the TripleA logo, brightened to read well on dark and light themes. */
  static final Color HIGHLIGHT = new Color(0xE5, 0x39, 0x35);

  private static final String STYLED = "LaunchScreenStyle.styled";
  private static final String KEYBOARD_FOCUS = "LaunchScreenStyle.keyboardFocus";
  // read and written on the event dispatch thread only
  private static boolean keyboardNavigationTracked;
  private static boolean keyboardNavigation;
  private static final ContainerListener STYLE_ADDED_CHILDREN =
      new ContainerAdapter() {
        @Override
        public void componentAdded(final ContainerEvent e) {
          install(e.getChild());
        }
      };

  /**
   * Styles every plain {@link JButton} under {@code component}, including buttons added later.
   * Subclasses such as the arrow buttons inside combo boxes, spinners and scroll bars keep their
   * look.
   */
  public static void install(final Component component) {
    trackKeyboardNavigation();
    if (component.getClass() == JButton.class) {
      styleAsText((JButton) component);
    }
    if (component instanceof Container container) {
      if (!Arrays.asList(container.getContainerListeners()).contains(STYLE_ADDED_CHILDREN)) {
        container.addContainerListener(STYLE_ADDED_CHILDREN);
      }
      for (final Component child : container.getComponents()) {
        install(child);
      }
    }
  }

  /** Styles a menu entry: enlarged clickable text with markers either side while highlighted. */
  public static void styleAsMenuItem(final JButton button, final float fontIncrease) {
    styleAsText(button);
    button.setFont(button.getFont().deriveFont(button.getFont().getSize2D() + fontIncrease));
    button.setBorder(new HighlightMarkerBorder());
  }

  private static void styleAsText(final JButton button) {
    if (Boolean.TRUE.equals(button.getClientProperty(STYLED))) {
      return;
    }
    button.putClientProperty(STYLED, true);
    button.setContentAreaFilled(false);
    button.setOpaque(false);
    button.setFocusPainted(false);
    button.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    button.setRolloverEnabled(true);
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    button.addChangeListener(e -> updateHighlight(button));
    button.addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(final FocusEvent e) {
            button.putClientProperty(KEYBOARD_FOCUS, keyboardNavigation);
            updateHighlight(button);
          }

          @Override
          public void focusLost(final FocusEvent e) {
            button.putClientProperty(KEYBOARD_FOCUS, false);
            updateHighlight(button);
          }
        });
  }

  /**
   * Tracks whether the user is navigating with Tab. Focus then gets the hover look so they can see
   * the selected entry. Focus Swing moves on its own, eg to the first button of a new screen after
   * a click, does not, or a button would light up for no visible reason.
   */
  private static void trackKeyboardNavigation() {
    if (keyboardNavigationTracked) {
      return;
    }
    keyboardNavigationTracked = true;
    Toolkit.getDefaultToolkit()
        .addAWTEventListener(
            event -> {
              if (event instanceof KeyEvent key
                  && key.getID() == KeyEvent.KEY_PRESSED
                  && key.getKeyCode() == KeyEvent.VK_TAB) {
                keyboardNavigation = true;
              } else if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                keyboardNavigation = false;
                // a click that leaves focus where it is must still drop the focus highlight
                if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
                        instanceof AbstractButton focused
                    && Boolean.TRUE.equals(focused.getClientProperty(KEYBOARD_FOCUS))) {
                  focused.putClientProperty(KEYBOARD_FOCUS, false);
                  updateHighlight(focused);
                }
              }
            },
            AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
  }

  private static void updateHighlight(final AbstractButton button) {
    // null falls back to the look and feel's text color
    button.setForeground(isHighlighted(button) ? HIGHLIGHT : null);
  }

  private static boolean isHighlighted(final AbstractButton button) {
    return button.isEnabled()
        && (button.getModel().isRollover()
            || Boolean.TRUE.equals(button.getClientProperty(KEYBOARD_FOCUS)));
  }

  /** Leaves room either side of the text and draws a small arrow in it while highlighted. */
  private static final class HighlightMarkerBorder extends AbstractBorder {
    private static final long serialVersionUID = 4321098765432109876L;
    private static final int MARKER_SPACE = 28;

    @Override
    public Insets getBorderInsets(final Component c, final Insets insets) {
      insets.set(4, MARKER_SPACE, 4, MARKER_SPACE);
      return insets;
    }

    @Override
    public void paintBorder(
        final Component c,
        final Graphics g,
        final int x,
        final int y,
        final int width,
        final int height) {
      if (!isHighlighted((AbstractButton) c)) {
        return;
      }
      final int size = Math.max(4, c.getFont().getSize() / 3);
      final int midY = y + height / 2;
      final int leftTip = x + MARKER_SPACE - 8;
      final int rightTip = x + width - MARKER_SPACE + 8;
      g.setColor(HIGHLIGHT);
      g.fillPolygon(
          new int[] {leftTip - size, leftTip, leftTip - size},
          new int[] {midY - size, midY, midY + size},
          3);
      g.fillPolygon(
          new int[] {rightTip + size, rightTip, rightTip + size},
          new int[] {midY - size, midY, midY + size},
          3);
    }
  }
}
