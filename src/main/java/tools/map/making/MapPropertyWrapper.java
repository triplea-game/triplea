package tools.map.making;

import java.awt.Color;
import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFrame;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.properties.AEditableProperty;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.CollectionProperty;
import games.strategy.engine.data.properties.ColorProperty;
import games.strategy.engine.data.properties.DoubleProperty;
import games.strategy.engine.data.properties.FileProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.MapProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.data.properties.StringProperty;
import games.strategy.util.PropertyUtil;
import games.strategy.util.Tuple;

/**
 * This will take ANY object, and then look at every method that begins with 'set[name]' and if there also exists a
 * method 'get[name]'
 * and a field '[name]' which is public, then it will take these and create an editable UI component
 * for each of these based on the games.strategy.engine.data.properties classes.
 *
 * @param <T>
 *        parameters can be: Boolean, String, Integer, Double, Color, File, Collection, Map
 */
@SuppressWarnings("unchecked")
public class MapPropertyWrapper<T> extends AEditableProperty {
  private static final long serialVersionUID = 6406798101396215624L;
  private IEditableProperty property;
  // private final Class m_clazz;
  // private final T m_defaultValue;
  private final Method setter;
  @SuppressWarnings("unused")
  private final Method getter;

  private MapPropertyWrapper(final String name, final String description, final T defaultValue, final Method setter,
      final Method getter) {
    super(name, description);
    // m_clazz = clazz;
    this.setter = setter;
    this.getter = getter;
    // m_defaultValue = defaultValue;
    if (defaultValue instanceof Boolean) {
      property = new BooleanProperty(name, description, ((Boolean) defaultValue));
    } else if (defaultValue instanceof Color) {
      property = new ColorProperty(name, description, ((Color) defaultValue));
    } else if (defaultValue instanceof File) {
      property = new FileProperty(name, description, ((File) defaultValue));
    } else if (defaultValue instanceof String) {
      property = new StringProperty(name, description, ((String) defaultValue));
    } else if (defaultValue instanceof Collection || defaultValue instanceof List || defaultValue instanceof Set) {
      property = new CollectionProperty<>(name, description, ((Collection<?>) defaultValue));
    } else if (defaultValue instanceof Map || defaultValue instanceof HashMap) {
      property = new MapProperty<>(name, description, ((Map<?, ?>) defaultValue));
    } else if (defaultValue instanceof Integer) {
      property =
          new NumberProperty(name, description, Integer.MAX_VALUE, Integer.MIN_VALUE, ((Integer) defaultValue));
    } else if (defaultValue instanceof Double) {
      property =
          new DoubleProperty(name, description, Double.MAX_VALUE, Double.MIN_VALUE, ((Double) defaultValue), 5);
    } else {
      throw new IllegalArgumentException(
          "Cannot instantiate PropertyWrapper with: " + defaultValue.getClass().getCanonicalName());
    }
  }

  @Override
  public int getRowsNeeded() {
    return property.getRowsNeeded();
  }

  @Override
  public Object getValue() {
    return property.getValue();
  }

  public T getValueT() {
    return (T) property.getValue();
  }

  @Override
  public void setValue(final Object value) throws ClassCastException {
    property.setValue(value);
  }

  public void setValueT(final T value) {
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
      System.out.println(setter + "   to   " + value);
      setter.invoke(object, args);
    } catch (final IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
      ClientLogger.logQuietly(e);
    }
  }

  private static List<MapPropertyWrapper<?>> createProperties(final Object object) {
    final List<MapPropertyWrapper<?>> properties = new ArrayList<>();
    for (final Method setter : object.getClass().getMethods()) {
      final boolean startsWithSet = setter.getName().startsWith("set");
      if (!startsWithSet) {
        continue;
      }
      final String propertyName =
          setter.getName().substring(Math.min(3, setter.getName().length()), setter.getName().length());
      final Method getter;
      try {
        getter = object.getClass().getMethod("get" + propertyName);
      } catch (final SecurityException e) {
        ClientLogger.logQuietly(e);
        continue;
      } catch (final NoSuchMethodException e) {
        ClientLogger.logQuietly(e);
        continue;
      }
      final String fieldName = Introspector.decapitalize(propertyName);
      final Field field = PropertyUtil.getFieldIncludingFromSuperClasses(object.getClass(), fieldName, false);
      final Object currentValue;
      try {
        currentValue = field.get(object);
      } catch (final IllegalArgumentException e) {
        ClientLogger.logQuietly(e);
        continue;
      } catch (final IllegalAccessException e) {
        ClientLogger.logQuietly(e);
        continue;
      }
      try {
        final MapPropertyWrapper<?> wrapper =
            new MapPropertyWrapper<>(propertyName, null, currentValue, setter, getter);
        properties.add(wrapper);
      } catch (final Exception e) {
        ClientLogger.logQuietly(e);
      }
    }
    properties.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
    return properties;
  }

  static void writePropertiesToObject(final Object object, final List<MapPropertyWrapper<?>> properties) {
    for (final MapPropertyWrapper<?> p : properties) {
      p.setToObject(object);
    }
  }

  public static PropertiesUI createPropertiesUI(final List<? extends IEditableProperty> properties,
      final boolean editable) {
    return new PropertiesUI(properties, editable);
  }

  static Tuple<PropertiesUI, List<MapPropertyWrapper<?>>> createPropertiesUI(final Object object,
      final boolean editable) {
    final List<MapPropertyWrapper<?>> properties = createProperties(object);
    final PropertiesUI ui = new PropertiesUI(properties, editable);
    return Tuple.of(ui, properties);
  }

  @Override
  public boolean validate(final Object value) {
    return property.validate(value);
  }

  public static void main(final String[] args) {
    final MapProperties mapProperties = new MapProperties();
    final List<MapPropertyWrapper<?>> properties = createProperties(mapProperties);
    final PropertiesUI ui = createPropertiesUI(properties, true);
    final JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(ui);
    frame.pack();
    frame.setVisible(true);
  }
}
