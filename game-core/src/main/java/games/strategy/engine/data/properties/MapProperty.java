package games.strategy.engine.data.properties;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComponent;

/**
 * Basically creates a map of other properties.
 *
 * @param <T> String or something with a valid toString()
 * @param <U> parameters can be: Boolean, String, Integer, Double, Color, File, Collection, Map
 */
public class MapProperty<T, U> extends AbstractEditableProperty<Map<T, U>> {
  private static final long serialVersionUID = -8021039503574228146L;

  private Map<T, U> map;
  final List<IEditableProperty<U>> properties = new ArrayList<>();

  public MapProperty(final String name, final String description, final Map<T, U> map) {
    super(name, description);
    this.map = map;
    resetProperties(map, properties, name, description);
  }

  @SuppressWarnings("unchecked")
  private void resetProperties(final Map<T, U> map, final List<IEditableProperty<U>> properties, final String name,
      final String description) {
    properties.clear();
    for (final Entry<T, U> entry : map.entrySet()) {
      final String key = (String) entry.getKey();
      final U value = entry.getValue();
      if (value instanceof Boolean) {
        properties.add((IEditableProperty<U>) new BooleanProperty(key, description, ((Boolean) value)));
      } else if (value instanceof Color) {
        properties.add((IEditableProperty<U>) new ColorProperty(key, description, ((Color) value)));
      } else if (value instanceof File) {
        properties.add((IEditableProperty<U>) new FileProperty(key, description, ((File) value)));
      } else if (value instanceof String) {
        properties.add((IEditableProperty<U>) new StringProperty(key, description, ((String) value)));
      } else if (value instanceof Collection) {
        properties.add((IEditableProperty<U>) new CollectionProperty<>(name, description, ((Collection<U>) value)));
      } else if (value instanceof Integer) {
        properties.add((IEditableProperty<U>) new NumberProperty(key, description,
            Integer.MAX_VALUE, Integer.MIN_VALUE, ((Integer) value)));
      } else if (value instanceof Double) {
        properties.add((IEditableProperty<U>) new DoubleProperty(key, description,
            Double.MAX_VALUE, Double.MIN_VALUE, ((Double) value), 5));
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
  public Map<T, U> getValue() {
    return map;
  }

  @Override
  public void setValue(final Map<T, U> value) {
    map = value;
    resetProperties(map, properties, this.getName(), this.getDescription());
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
  public boolean validate(final Map<T, U> value) {
    if (value == null) {
      // is this ok? no idea, no maps or anything use this
      return false;
    }
    try {
      if (map != null && !map.isEmpty() && !value.isEmpty()) {
        T key = null;
        U val = null;
        for (final Entry<T, U> entry : map.entrySet()) {
          if (entry.getValue() != null && entry.getKey() != null) {
            key = entry.getKey();
            val = entry.getValue();
            break;
          }
        }
        if (key != null) {
          for (final Entry<T, U> entry : value.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null
                && (!entry.getKey().getClass().isAssignableFrom(key.getClass())
                    || !entry.getValue().getClass().isAssignableFrom(val.getClass()))) {
              return false;
            }
          }
        }
      }
      final List<IEditableProperty<U>> testProps = new ArrayList<>();
      resetProperties(value, testProps, this.getName(), this.getDescription());
    } catch (final Exception e) {
      return false;
    }
    return true;
  }
}
