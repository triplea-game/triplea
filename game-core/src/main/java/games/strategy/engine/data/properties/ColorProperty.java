package games.strategy.engine.data.properties;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * User editable property representing a color.
 *
 * <p>Presents a clickable label with the currently selected color, through which a color swatch
 * panel is accessable to change the color.
 */
public class ColorProperty extends AbstractEditableProperty<Color> {
  private static final long serialVersionUID = 6826763550643504789L;

  private Color color;

  public ColorProperty(final String name, final String description, final Color def) {
    super(name, description);
    color = def == null ? Color.black : def;
  }

  @Override
  public Color getValue() {
    return color;
  }

  @Override
  public void setValue(final Color value) {
    color = value == null ? Color.black : value;
  }

  @Override
  public JComponent getEditorComponent() {
    final JLabel label =
        new JLabel("        ") {
          private static final long serialVersionUID = 3833935337866905836L;

          @Override
          public void paintComponent(final Graphics g) {
            final Graphics2D g2 = (Graphics2D) g;
            g2.setColor(color);
            g2.fill(g2.getClip());
          }
        };
    label.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(final MouseEvent e) {
            final Color colorSelected =
                JColorChooser.showDialog(
                    label,
                    "Choose color",
                    (ColorProperty.this.color == null ? Color.black : ColorProperty.this.color));
            if (colorSelected != null) {
              color = colorSelected;
              // Ask Swing to repaint this label when it's convenient
              SwingUtilities.invokeLater(label::repaint);
            }
          }
        });
    return label;
  }

  @Override
  public boolean validate(final Object value) {
    return (value == null) || (value instanceof Color);
  }
}
