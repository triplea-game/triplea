package games.strategy.engine.data.properties;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 * A string property with a simple text field editor
 */
public class StringProperty extends AEditableProperty {
  private static final long serialVersionUID = 4382624884674152208L;
  private String m_value;

  public StringProperty(final String name, final String description, final String defaultValue) {
    super(name, description);
    m_value = defaultValue;
  }

  @Override
  public JComponent getEditorComponent() {
    final JTextField text = new JTextField(m_value);
    text.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        m_value = text.getText();
      }
    });
    text.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(final FocusEvent e) {}

      @Override
      public void focusLost(final FocusEvent e) {
        m_value = text.getText();
      }
    });
    final Dimension ourMinimum = new Dimension(80, 20);
    text.setMinimumSize(ourMinimum);
    text.setPreferredSize(ourMinimum);
    return text;
  }

  @Override
  public Object getValue() {
    return m_value;
  }

  @Override
  public void setValue(final Object value) throws ClassCastException {
    m_value = (String) value;
  }

  @Override
  public boolean validate(final Object value) {
    if (value == null) {
      return true;
    }
    return value instanceof String;
  }
}
