package tools.map.making;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.properties.AbstractEditableProperty;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.ColorProperty;
import games.strategy.engine.data.properties.DoubleProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.MapProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.PropertiesUi;
import games.strategy.engine.data.properties.StringProperty;
import java.awt.Color;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import lombok.extern.slf4j.Slf4j;
import org.triplea.util.Tuple;

/**
 * This will take ANY object, and then look at every method that begins with 'set[name]' and if
 * there also exists a method 'get[name]' and a field '[name]' which is public, then it will take
 * these and create an editable UI component for each of these based on the
 * games.strategy.engine.data.properties classes.
 *
 * @param <T> parameters can be: Boolean, String, Integer, Double, Color, File, Collection, Map
 */
@Slf4j
public class MapPropertyWrapper<T> extends AbstractEditableProperty<T> {
  private static final long serialVersionUID = 6406798101396215624L;

  private final IEditableProperty<T> property;
  private final Method setter;

  @SuppressWarnings("unchecked")
  private MapPropertyWrapper(
      final String name, final String description, final T defaultValue, final Method setter) {
    super(name, description);
    this.setter = setter;

    final IEditableProperty<?> property;

    if (defaultValue instanceof Boolean) {
      property = new BooleanProperty(name, description, ((Boolean) defaultValue));
    } else if (defaultValue instanceof Color) {
      property = new ColorProperty(name, description, ((Color) defaultValue));
    } else if (defaultValue instanceof String) {
      property = new StringProperty(name, description, ((String) defaultValue));
    } else if (defaultValue instanceof Map) {
      property = new MapProperty<>(name, description, ((Map<String, ?>) defaultValue));
    } else if (defaultValue instanceof Integer) {
      property =
          new NumberProperty(
              name, description, Integer.MAX_VALUE, Integer.MIN_VALUE, ((Integer) defaultValue));
    } else if (defaultValue instanceof Double) {
      property =
          new DoubleProperty(
              name, description, Double.MAX_VALUE, Double.MIN_VALUE, ((Double) defaultValue), 5);
    } else {
      throw new IllegalArgumentException(
          "Cannot instantiate PropertyWrapper with: " + defaultValue.getClass().getCanonicalName());
    }
    this.property = (IEditableProperty<T>) property;
  }

  @Override
  public int getRowsNeeded() {
    return property.getRowsNeeded();
  }

  @Override
  public T getValue() {
    return property.getValue();
  }

  @Override
  public void setValue(final T value) {
    property.setValue(value);
  }

  @Override
  public JComponent getEditorComponent() {
    return property.getEditorComponent();
  }

  private void setToObject(final Object object) {
    final Object value = getValue();
    final Object[] args = {value};
    try {
      log.info("{}   to   {}", setter, value);
      setter.invoke(object, args);
    } catch (final IllegalArgumentException
        | InvocationTargetException
        | IllegalAccessException e) {
      log.error("Failed to invoke setter reflectively: {}", setter.getName(), e);
    }
  }

  private static List<MapPropertyWrapper<?>> newProperties(final Object object) {
    final List<MapPropertyWrapper<?>> properties = new ArrayList<>();
    for (final Method setter : object.getClass().getMethods()) {
      final boolean startsWithSet = setter.getName().startsWith("set");
      if (!startsWithSet) {
        continue;
      }
      final String propertyName =
          setter.getName().substring(Math.min(3, setter.getName().length()));

      final String fieldName = Introspector.decapitalize(propertyName);
      final Field field = getPropertyField(fieldName, object.getClass());
      final Object currentValue;
      try {
        currentValue = field.get(object);
      } catch (final IllegalArgumentException | IllegalAccessException e) {
        log.error("Failed to get field value reflectively: {}", field.getName(), e);
        continue;
      }
      try {
        final MapPropertyWrapper<?> wrapper =
            new MapPropertyWrapper<>(propertyName, null, currentValue, setter);
        properties.add(wrapper);
      } catch (final Exception e) {
        log.error("Failed to create map property wrapper", e);
      }
    }
    properties.sort(Comparator.comparing(MapPropertyWrapper::getName));
    return properties;
  }

  static void writePropertiesToObject(
      final Object object, final List<MapPropertyWrapper<?>> properties) {
    for (final MapPropertyWrapper<?> p : properties) {
      p.setToObject(object);
    }
  }

  static Tuple<PropertiesUi, List<MapPropertyWrapper<?>>> newPropertiesUi(
      final Object object, final boolean editable) {
    final List<MapPropertyWrapper<?>> properties = newProperties(object);
    final PropertiesUi ui = new PropertiesUi(properties, editable);
    return Tuple.of(ui, properties);
  }

  @Override
  public boolean validate(final Object value) {
    return property.validate(value);
  }

  private static Field getFieldIncludingFromSuperClasses(
      final Class<?> c, final String name, final boolean justFromSuper) {
    if (!justFromSuper) {
      try {
        return c.getDeclaredField(name); // TODO: unchecked reflection
      } catch (final NoSuchFieldException e) {
        return getFieldIncludingFromSuperClasses(c, name, true);
      }
    }

    if (c.getSuperclass() == null) {
      throw new IllegalStateException("No such Property Field: " + name);
    }
    try {
      return c.getSuperclass().getDeclaredField(name); // TODO: unchecked reflection
    } catch (final NoSuchFieldException e) {
      return getFieldIncludingFromSuperClasses(c.getSuperclass(), name, true);
    }
  }

  /**
   * Gets the backing field for the property with the specified name in the specified type.
   *
   * @param propertyName The property name.
   * @param type The type that hosts the property.
   * @return The backing field for the specified property.
   * @throws IllegalStateException If no backing field for the specified property exists.
   */
  @VisibleForTesting
  static Field getPropertyField(final String propertyName, final Class<?> type) {
    return getFieldIncludingFromSuperClasses(type, propertyName, false);
  }
}
