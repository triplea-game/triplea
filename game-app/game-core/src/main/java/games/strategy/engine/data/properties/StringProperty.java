package games.strategy.engine.data.properties;

import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JComponent;
import javax.swing.JTextField;

/** A string property with a simple text field editor. */
public class StringProperty extends AbstractEditableProperty<String> {
  private static final long serialVersionUID = 4382624884674152208L;

  private String value;

  public StringProperty(final String name, final String description, final String defaultValue) {
    super(name, description);
    value = defaultValue;
  }

  @Override
  public JComponent getEditorComponent() {
    final JTextField text = new JTextField(value);
    text.addActionListener(e -> value = text.getText());
    text.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(final FocusEvent e) {}

          @Override
          public void focusLost(final FocusEvent e) {
            value = text.getText();
          }
        });
    final Dimension ourMinimum = new Dimension(80, 20);
    text.setMinimumSize(ourMinimum);
    text.setPreferredSize(ourMinimum);
    return text;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public void setValue(final String value) {
    this.value = value;
  }

  @Override
  public boolean validate(final Object value) {
    return value == null || value instanceof String;
  }
}
