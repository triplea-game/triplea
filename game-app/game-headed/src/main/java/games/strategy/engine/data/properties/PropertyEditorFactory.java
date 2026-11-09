package games.strategy.engine.data.properties;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.swing.IntTextField;

/** Builds the Swing components that edit or display an {@link IEditableProperty}. */
@UtilityClass
public class PropertyEditorFactory {

  /** Returns a component that edits the given property in place. */
  public static JComponent newEditorComponent(final IEditableProperty<?> property) {
    return switch (property) {
      case BooleanProperty booleanProperty -> newBooleanEditor(booleanProperty);
      case ColorProperty colorProperty -> newColorEditor(colorProperty);
      case DoubleProperty doubleProperty -> newDoubleEditor(doubleProperty);
      case MapProperty<?> mapProperty -> new PropertiesUi(mapProperty.getProperties(), true);
      case NumberProperty numberProperty -> newNumberEditor(numberProperty);
      case StringProperty stringProperty -> newStringEditor(stringProperty);
      default ->
          throw new IllegalArgumentException(
              "No editor for property type: " + property.getClass().getName());
    };
  }

  /** Returns a read-only component that displays the given property. */
  public static JComponent newViewComponent(final IEditableProperty<?> property) {
    if (property instanceof MapProperty<?> mapProperty) {
      return new PropertiesUi(mapProperty.getProperties(), false);
    }
    final JComponent component = newEditorComponent(property);
    component.setEnabled(false);
    return component;
  }

  private static JComponent newBooleanEditor(final BooleanProperty property) {
    final JCheckBox box = new JCheckBox("");
    box.setSelected(property.getValue());
    box.addActionListener(e -> property.setValue(box.isSelected()));
    return box;
  }

  private static JComponent newColorEditor(final ColorProperty property) {
    final JLabel label =
        new JLabel("        ") {
          private static final long serialVersionUID = 3833935337866905836L;

          @Override
          public void paintComponent(final Graphics g) {
            final Graphics2D g2 = (Graphics2D) g;
            g2.setColor(property.getValue());
            g2.fill(g2.getClip());
          }
        };
    label.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(final MouseEvent e) {
            final Color colorSelected =
                JColorChooser.showDialog(label, "Choose color", property.getValue());
            if (colorSelected != null) {
              property.setValue(colorSelected);
              SwingUtilities.invokeLater(label::repaint);
            }
          }
        });
    return label;
  }

  private static JComponent newDoubleEditor(final DoubleProperty property) {
    final JSpinner field =
        new JSpinner(
            new SpinnerNumberModel(
                property.getValue().doubleValue(), property.getMin(), property.getMax(), 1.0));

    // NB: Workaround for JSpinner default sizing algorithm when min/max values have very large
    // magnitudes
    // (see: https://implementsblog.com/2012/11/26/java-gotcha-jspinner-preferred-size/)
    final JComponent fieldEditor = field.getEditor();
    if (fieldEditor instanceof JSpinner.DefaultEditor) {
      ((JSpinner.DefaultEditor) fieldEditor).getTextField().setColumns(10);
    }

    field.addChangeListener(e -> property.setValue((Double) field.getValue()));
    return field;
  }

  private static JComponent newNumberEditor(final NumberProperty property) {
    final IntTextField intTextField = new IntTextField(property.getMin(), property.getMax());
    intTextField.setValue(property.getValue());
    intTextField.addChangeListener(field -> property.setValue(field.getValue()));
    return intTextField;
  }

  private static JComponent newStringEditor(final StringProperty property) {
    final JTextField text = new JTextField(property.getValue());
    text.addActionListener(e -> property.setValue(text.getText()));
    text.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(final FocusEvent e) {}

          @Override
          public void focusLost(final FocusEvent e) {
            property.setValue(text.getText());
          }
        });
    final Dimension ourMinimum = new Dimension(80, 20);
    text.setMinimumSize(ourMinimum);
    text.setPreferredSize(ourMinimum);
    return text;
  }
}
