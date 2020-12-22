package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.properties.GameProperties;

class SetPropertyChange extends Change {
  private static final long serialVersionUID = -1377597975513821508L;

  private final String property;
  private final Object value;
  private final Object oldValue;

  SetPropertyChange(final String property, final Object value, final GameProperties properties) {
    this.property = property;
    this.value = value;
    oldValue = properties.get(property);
  }

  private SetPropertyChange(final String property, final Object value, final Object oldValue) {
    this.property = property;
    this.value = value;
    this.oldValue = oldValue;
  }

  @Override
  public Change invert() {
    return new SetPropertyChange(property, oldValue, value);
  }

  @Override
  protected void perform(final GameState data) {
    data.getProperties().set(property, value);
  }
}
