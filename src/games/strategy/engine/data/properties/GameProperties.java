package games.strategy.engine.data.properties;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataComponent;

/**
 * Properties of the current game. <br>
 * Maps string -> Object <br>
 * Set through changeFactory.setProperty.
 *
 */
public class GameProperties extends GameDataComponent {
  private static final long serialVersionUID = -1448163357090677564L;
  private final Map<String, Object> m_constantProperties = new HashMap<String, Object>();
  // a set of IEditableProperties
  private final Map<String, IEditableProperty> m_editableProperties = new HashMap<String, IEditableProperty>();
  // This list is used to keep track of order properties were
  // added.
  private final List<String> m_ordering = new ArrayList<String>();

  /**
   * Creates a new instance of Properties
   *
   * @param data
   *        game data
   */
  public GameProperties(final GameData data) {
    super(data);
  }

  /**
   * Setting a property to null has the effect of unbinding the key.
   * package access to prevent outsiders from setting properties
   *
   * @param key
   *        key of property
   * @param value
   *        property
   */
  public void set(final String key, final Object value) {
    // TODO should only accept serializable, not object
    if (value == null) {
      m_constantProperties.remove(key);
      m_ordering.remove(key);
    } else {
      m_constantProperties.put(key, value);
      m_ordering.add(key);
    }
  }

  /**
   *
   * @param key
   *        referring key
   * @return property with key or null if property is not contained in the list
   *         (The object returned should not be modified, as modifications will not appear globally.)
   */
  public Object get(final String key) {
    if (m_editableProperties.containsKey(key)) {
      return m_editableProperties.get(key).getValue();
    }
    return m_constantProperties.get(key);
  }

  public boolean get(final String key, final boolean defaultValue) {
    final Object value = get(key);
    if (value == null) {
      return defaultValue;
    }
    return (Boolean) value;
  }

  public int get(final String key, final int defaultValue) {
    final Object value = get(key);
    if (value == null) {
      return defaultValue;
    }
    return (Integer) value;
  }

  public String get(final String key, final String defaultValue) {
    final Object value = get(key);
    if (value == null) {
      return defaultValue;
    }
    return (String) value;
  }

  public Object get(final String key, final Object defaultValue) {
    final Object value = get(key);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  public void addEditableProperty(final IEditableProperty property) {
    // add to the editable properties
    m_editableProperties.put(property.getName(), property);
    m_ordering.add(property.getName());
  }

  /**
   * Return list of editable properties in the order they were added.
   *
   * @return a list of IEditableProperty
   */
  public List<IEditableProperty> getEditableProperties() {
    final List<IEditableProperty> properties = new ArrayList<IEditableProperty>();
    for (final String propertyName : m_ordering) {
      if (m_editableProperties.containsKey(propertyName)) {
        properties.add(m_editableProperties.get(propertyName));
      }
    }
    return properties;
  }


  public static void toOutputStream(final OutputStream sink, final List<IEditableProperty> editableProperties) throws IOException {
    // write internally first in case of error
    final ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
    ObjectOutputStream outStream = null;
    GZIPOutputStream zippedOut = null;
    try {
      outStream = new ObjectOutputStream(bos);
      outStream.writeObject(editableProperties);
      // final byte[] byteArray = bos.toByteArray();
      zippedOut = new GZIPOutputStream(sink);
      zippedOut.write(bos.toByteArray());// now write to file
      zippedOut.flush();
    } finally {
      try {
        if (outStream != null) {
          outStream.close();
        }
      } catch (final IOException e) {
        e.printStackTrace();
      }
      try {
        bos.close();
      } catch (final IOException e) {
        e.printStackTrace();
      }
      try {
        if (zippedOut != null) {
          zippedOut.close();
        }
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static List<IEditableProperty> streamToIEditablePropertiesList(final byte[] byteArray)
      throws IOException, ClassNotFoundException, ClassCastException {
    ByteArrayInputStream byteStream = null;
    InputStream inputStream = null;
    ObjectInputStream objectStream = null;
    List<IEditableProperty> editableProperties = null;
    try {
      byteStream = new ByteArrayInputStream(byteArray);
      inputStream = new BufferedInputStream(byteStream);
      objectStream = new ObjectInputStream(new GZIPInputStream(inputStream));
      editableProperties = (List<IEditableProperty>) objectStream.readObject();
    } finally {
      if (byteStream != null) {
        try {
          byteStream.close();
        } catch (final IOException e) {
          e.printStackTrace();
        }
      }
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (final IOException e) {
          e.printStackTrace();
        }
      }
      if (objectStream != null) {
        try {
          objectStream.close();
        } catch (final IOException e) {
          e.printStackTrace();
        }
      }
    }
    return editableProperties;
  }

  public static void applyByteMapToChangeProperties(final byte[] byteArray, final GameProperties gamePropertiesToBeChanged) {
    List<IEditableProperty> editableProperties = null;
    try {
      editableProperties = streamToIEditablePropertiesList(byteArray);
    } catch (final ClassNotFoundException e) {
      e.printStackTrace();
    } catch (final ClassCastException e) {
      e.printStackTrace();
    } catch (final IOException e) {
      e.printStackTrace();
    }
    applyListToChangeProperties(editableProperties, gamePropertiesToBeChanged);
  }

  public static void applyListToChangeProperties(final List<IEditableProperty> editableProperties,
      final GameProperties gamePropertiesToBeChanged) {
    if (editableProperties == null || editableProperties.isEmpty()) {
      return;
    }
    for (final IEditableProperty prop : editableProperties) {
      if (prop == null || prop.getName() == null) {
        continue;
      }
      final IEditableProperty p = gamePropertiesToBeChanged.m_editableProperties.get(prop.getName());
      if (p != null && prop.getName().equals(p.getName()) && p.validate(prop.getValue())) {
        p.setValue(prop.getValue());
      }
    }
  }
}
