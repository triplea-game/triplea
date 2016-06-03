package games.strategy.engine.data.properties;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

public class BooleanProperty extends AEditableProperty {
  // compatible with 0.9.0.2 saved games
  private static final long serialVersionUID = -7265501762343216435L;
  private boolean mValue;

  public BooleanProperty(final String name, final String description, final boolean defaultValue) {
    super(name, description);
    mValue = defaultValue;
  }

  @Override
  public Object getValue() {
    return mValue ? Boolean.TRUE : Boolean.FALSE;
  }

  @Override
  public void setValue(final Object value) throws IllegalArgumentException {
    mValue = (Boolean) value;
  }

  public void setValue(final boolean value) {
    mValue = value;
  }

  /**
   * @return component used to edit this property
   */
  @Override
  public JComponent getEditorComponent() {
    final JCheckBox box = new JCheckBox("");
    box.setSelected(mValue);
    box.addActionListener(e -> mValue = box.isSelected());
    return box;
  }

  @Override
  public boolean validate(final Object value) {
    if (value instanceof Boolean) {
      return true;
    }
    return false;
  }
}
