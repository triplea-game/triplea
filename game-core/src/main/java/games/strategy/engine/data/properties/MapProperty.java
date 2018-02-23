package games.strategy.engine.data.properties;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JComponent;

/**
 * Basically creates a map of other properties.
 *
 * @param <T>
 *        String or something with a valid toString()
 * @param <U>
 *        parameters can be: Boolean, String, Integer, Double, Color, File, Collection, Map
 */
public class MapProperty<T, U> extends AEditableProperty {
  private static final long serialVersionUID = -8021039503574228146L;
  private Map<T, U> m_map;
  final List<IEditableProperty> m_properties = new ArrayList<>();

  public MapProperty(final String name, final String description, final Map<T, U> map) {
    super(name, description);
    m_map = map;
    resetProperties(map, m_properties, name, description);
  }

  @SuppressWarnings("unchecked")
  private void resetProperties(final Map<T, U> map, final List<IEditableProperty> properties, final String name,
      final String description) {
    properties.clear();
    for (final Entry<T, U> entry : map.entrySet()) {
      final String key = (String) entry.getKey();
      final U value = entry.getValue();
      if (value instanceof Boolean) {
        properties.add(new BooleanProperty(key, description, ((Boolean) value)));
      } else if (value instanceof Color) {
        properties.add(new ColorProperty(key, description, ((Color) value)));
      } else if (value instanceof File) {
        properties.add(new FileProperty(key, description, ((File) value)));
      } else if (value instanceof String) {
        properties.add(new StringProperty(key, description, ((String) value)));
      } else if ((value instanceof Collection) || (value instanceof List) || (value instanceof Set)) {
        properties.add(new CollectionProperty<>(name, description, ((Collection<U>) value)));
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
    return Math.max(1, m_properties.size());
  }

  @Override
  public Object getValue() {
    return m_map;
  }

  public Map<T, U> getValueT() {
    return m_map;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setValue(final Object value) throws ClassCastException {
    m_map = (Map<T, U>) value;
    resetProperties(m_map, m_properties, this.getName(), this.getDescription());
  }

  public void setValueT(final Map<T, U> value) {
    m_map = value;
    resetProperties(m_map, m_properties, this.getName(), this.getDescription());
  }

  @Override
  public JComponent getEditorComponent() {
    return new PropertiesUi(m_properties, true);
  }

  @Override
  public JComponent getViewComponent() {
    return new PropertiesUi(m_properties, false);
  }

  @Override
  public boolean validate(final Object value) {
    if (value == null) {
      // is this ok? no idea, no maps or anything use this
      return false;
    }
    if (Map.class.isAssignableFrom(value.getClass())) {
      try {
        @SuppressWarnings("unchecked")
        final Map<T, U> test = (Map<T, U>) value;
        if ((m_map != null) && !m_map.isEmpty() && !test.isEmpty()) {
          T key = null;
          U val = null;
          for (final Entry<T, U> entry : m_map.entrySet()) {
            if ((entry.getValue() != null) && (entry.getKey() != null)) {
              key = entry.getKey();
              val = entry.getValue();
              break;
            }
          }
          if ((key != null) && (val != null)) {
            for (final Entry<T, U> entry : test.entrySet()) {
              if ((entry.getKey() != null) && (entry.getValue() != null)
                  && (!entry.getKey().getClass().isAssignableFrom(key.getClass())
                  || !entry.getValue().getClass().isAssignableFrom(val.getClass()))) {
                return false;
              }
            }
          }
        }
        final List<IEditableProperty> testProps = new ArrayList<>();
        resetProperties(test, testProps, this.getName(), this.getDescription());
      } catch (final Exception e) {
        return false;
      }
      return true;
    }
    return false;
  }
}
