package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Unit;
import java.io.IOException;
import java.io.ObjectInputStream;
import lombok.AccessLevel;
import lombok.Getter;

/** A game data change that captures a change to an object property value. */
public class ObjectPropertyChange extends Change {
  private static final long serialVersionUID = 4218093376094170940L;

  private final Unit object;
  @Getter private String property;

  @Getter(AccessLevel.PACKAGE)
  private final Object newValue;

  @Getter(AccessLevel.PACKAGE)
  private final Object oldValue;

  ObjectPropertyChange(final Unit object, final String property, final Object newValue) {
    this.object = object;
    this.property = property.intern();
    this.newValue = newValue;
    oldValue = object.getPropertyOrThrow(property).getValue();
  }

  private ObjectPropertyChange(
      final Unit object, final String property, final Object newValue, final Object oldValue) {
    this.object = object;
    // prevent multiple copies of the property names being held in the game
    this.property = property.intern();
    this.newValue = newValue;
    this.oldValue = oldValue;
  }

  private void readObject(final ObjectInputStream stream)
      throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    property = property.intern();
  }

  @Override
  public Change invert() {
    return new ObjectPropertyChange(object, property, oldValue, newValue);
  }

  @Override
  protected void perform(final GameDataInjections data) {
    try {
      object.getPropertyOrThrow(property).setValue(newValue);
    } catch (final MutableProperty.InvalidValueException e) {
      throw new IllegalStateException(
          String.format(
              "failed to set value '%s' on property '%s' for object '%s'",
              newValue, property, object),
          e);
    }
  }

  @Override
  public String toString() {
    return "Property change, unit:"
        + object
        + " property:"
        + property
        + " newValue:"
        + newValue
        + " oldValue:"
        + oldValue;
  }
}
