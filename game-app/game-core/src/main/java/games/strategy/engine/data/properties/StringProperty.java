package games.strategy.engine.data.properties;

/** Implementation of {@link IEditableProperty} for a string value. */
public class StringProperty extends AbstractEditableProperty<String> {
  private static final long serialVersionUID = 4382624884674152208L;

  private String value;

  public StringProperty(final String name, final String description, final String defaultValue) {
    super(name, description);
    value = defaultValue;
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
