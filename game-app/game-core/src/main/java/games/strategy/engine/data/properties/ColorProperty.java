package games.strategy.engine.data.properties;

import java.awt.Color;

/** User editable property representing a color. */
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
  public boolean validate(final Object value) {
    return (value == null) || (value instanceof Color);
  }
}
