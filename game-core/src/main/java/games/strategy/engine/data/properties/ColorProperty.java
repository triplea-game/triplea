package games.strategy.engine.data.properties;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * User editable property representing a color.
 *
 * <p>
 * Presents a clickable label with the currently selected color, through which a color swatch panel is accessable to
 * change the color.
 * </p>
 */
public class ColorProperty extends AEditableProperty {
  private static final long serialVersionUID = 6826763550643504789L;
  private static final int MAX_COLOR = 0xFFFFFF;
  private static final int MIN_COLOR = 0x000000;

  private Color color;

  public ColorProperty(final String name, final String description, final int def) {
    super(name, description);
    if ((def > MAX_COLOR) || (def < MIN_COLOR)) {
      throw new IllegalArgumentException("Default value out of range");
    }
    color = new Color(def);
  }

  public ColorProperty(final String name, final String description, final Color def) {
    super(name, description);
    if (def == null) {
      color = Color.black;
    } else {
      color = def;
    }
  }

  @Override
  public Object getValue() {
    return color;
  }

  @Override
  public void setValue(final Object value) throws ClassCastException {
    if (value == null) {
      color = Color.black;
    } else {
      color = (Color) value;
    }
  }

  @Override
  public JComponent getEditorComponent() {
    final JLabel label = new JLabel("        ") {
      private static final long serialVersionUID = 3833935337866905836L;

      @Override
      public void paintComponent(final Graphics g) {
        final Graphics2D g2 = (Graphics2D) g;
        g2.setColor(color);
        g2.fill(g2.getClip());
      }
    };
    label.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        try {
          final Color colorSelected =
              JColorChooser.showDialog(label, "Choose color",
                  ((ColorProperty.this.color == null) ? Color.black : ColorProperty.this.color));
          if (colorSelected != null) {
            color = colorSelected;
            // Ask Swing to repaint this label when it's convenient
            SwingUtilities.invokeLater(() -> label.repaint());
          }
        } catch (final Exception exception) {
          System.err.println(exception.getMessage());
        }
      }

      @Override
      public void mouseEntered(final MouseEvent e) {}

      @Override
      public void mouseExited(final MouseEvent e) {}

      @Override
      public void mousePressed(final MouseEvent e) {}

      @Override
      public void mouseReleased(final MouseEvent e) {}
    });
    return label;
  }

  @Override
  public boolean validate(final Object value) {
    return (value == null) || (value instanceof Color);
  }
}
