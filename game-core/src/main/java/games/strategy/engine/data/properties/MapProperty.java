package games.strategy.engine.data.properties;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

/**
 * Basically creates a map of other properties.
 *
 * @param <V> parameters can be: Boolean, String, Integer, Double, Color, File, Collection, Map
 */
public class MapProperty<V> extends AbstractEditableProperty<Map<String, V>> {
  private static final long serialVersionUID = -8021039503574228146L;

  private Map<String, V> map;
  private final List<IEditableProperty<?>> properties = new ArrayList<>();

  public MapProperty(final String name, final String description, final Map<String, V> map) {
    super(name, description);
    this.map = map;
    resetProperties(map, properties, name, description);
  }

  private void resetProperties(
      final Map<String, V> map,
      final List<IEditableProperty<?>> properties,
      final String name,
      final String description) {
    properties.clear();
    for (final Map.Entry<String, V> entry : map.entrySet()) {
      final String key = entry.getKey();
      final V value = entry.getValue();
      if (value instanceof Boolean) {
        properties.add(new BooleanProperty(key, description, ((Boolean) value)));
      } else if (value instanceof Color) {
        properties.add(new ColorProperty(key, description, ((Color) value)));
      } else if (value instanceof File) {
        properties.add(new FileProperty(key, description, ((File) value)));
      } else if (value instanceof String) {
        properties.add(new StringProperty(key, description, ((String) value)));
      } else if (value instanceof Collection) {
        properties.add(new CollectionProperty<>(name, description, ((Collection<?>) value)));
      } else if (value instanceof Integer) {
        properties.add(new NumberProperty(key, description, Integer.MAX_VALUE, Integer.MIN_VALUE, ((Integer) value)));
      } else if (value instanceof Double) {
        properties.add(new DoubleProperty(key, description, Double.MAX_VALUE, Double.MIN_VALUE, ((Double) value), 5));
      } else {
        throw new IllegalArgumentException(
            "Cannot instantiate MapProperty with: " + value.getClass().getCanonicalName());
      }
    }
  }

  @Override
  public int getRowsNeeded() {
    return Math.max(1, properties.size());
  }

  @Override
  public Map<String, V> getValue() {
    return map;
  }

  @Override
  public void setValue(final Map<String, V> value) {
    map = value;
    resetProperties(map, properties, getName(), getDescription());
  }

  @Override
  public JComponent getEditorComponent() {
    return new PropertiesUi(properties, true);
  }

  @Override
  public JComponent getViewComponent() {
    return new PropertiesUi(properties, false);
  }

  @Override
  public boolean validate(final Object value) {
    if (value instanceof Map) {
      try {
        @SuppressWarnings("unchecked")
        final Map<String, V> test = (Map<String, V>) value;
        if (map != null && !map.isEmpty() && !test.isEmpty()) {
          String key = null;
          V val = null;
          for (final Map.Entry<String, V> entry : map.entrySet()) {
            if (entry.getValue() != null && entry.getKey() != null) {
              key = entry.getKey();
              val = entry.getValue();
              break;
            }
          }
          if (key != null) {
            for (final Map.Entry<String, V> entry : test.entrySet()) {
              if (entry.getKey() != null && entry.getValue() != null
                  && (!entry.getKey().getClass().isAssignableFrom(key.getClass())
                      || !entry.getValue().getClass().isAssignableFrom(val.getClass()))) {
                return false;
              }
            }
          }
        }
        resetProperties(test, new ArrayList<>(), getName(), getDescription());
      } catch (final Exception e) {
        return false;
      }
      return true;
    }
    return false;
  }
}
