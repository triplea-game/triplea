package games.strategy.engine.data.properties;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

/** Implementation of {@link IEditableProperty} for a Boolean value. */
public class BooleanProperty extends AbstractEditableProperty<Boolean> {
  private static final long serialVersionUID = -7265501762343216435L;

  private boolean value;

  public BooleanProperty(final String name, final String description, final boolean defaultValue) {
    super(name, description);
    value = defaultValue;
  }

  @Override
  public Boolean getValue() {
    return value;
  }

  @Override
  public void setValue(final Boolean value) {
    this.value = value;
  }

  public void setValue(final boolean value) {
    this.value = value;
  }

  @Override
  public JComponent getEditorComponent() {
    final JCheckBox box = new JCheckBox("");
    box.setSelected(value);
    box.addActionListener(e -> value = box.isSelected());
    return box;
  }

  @Override
  public boolean validate(final Object value) {
    return value instanceof Boolean;
  }
}
