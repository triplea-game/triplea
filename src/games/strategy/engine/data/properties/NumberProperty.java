package games.strategy.engine.data.properties;

import java.io.File;

import javax.swing.JComponent;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.ui.IntTextField;
import games.strategy.ui.IntTextFieldChangeListener;

public class NumberProperty extends AEditableProperty {
  // compatible with 0.9.0.2 saved games
  private static final long serialVersionUID = 6826763550643504789L;
  private final int m_max;
  private final int m_min;
  private int m_value;

  public NumberProperty(final String name, final String description, final int max, final int min, final int def) {
    super(name, description);
    if (max < min) {
      throw new IllegalThreadStateException("Max must be greater than min");
    }
    if (def > max || def < min) {
      throw new IllegalThreadStateException("Default value out of range");
    }
    m_max = max;
    m_min = min;
    m_value = def;
  }

  @Override
  public Integer getValue() {
    return m_value;
  }

  @Override
  public void setValue(final Object value) throws ClassCastException {
    if (value instanceof String) {
      // warn developer which have run with the option cache when Number properties were stored as strings
      // todo (kg) remove at a later point
      throw new RuntimeException(
          "Number properties are no longer stored as Strings. You should delete your option cache, located at "
              + new File(ClientFileSystemHelper.getUserRootFolder(), "optionCache").toString());
    } else {
      m_value = (Integer) value;
    }
  }

  @Override
  public JComponent getEditorComponent() {
    final IntTextField field = new IntTextField(m_min, m_max);
    field.setValue(m_value);
    field.addChangeListener(new IntTextFieldChangeListener() {
      @Override
      public void changedValue(final IntTextField aField) {
        m_value = aField.getValue();
      }
    });
    return field;
  }

  @Override
  public boolean validate(final Object value) {
    if (value instanceof Integer) {
      final int i = ((Integer) value);
      if (i <= m_max && i >= m_min) {
        return true;
      }
    }
    return false;
  }
}
