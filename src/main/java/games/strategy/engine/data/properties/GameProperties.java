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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataComponent;

/**
 * Properties of the current game. <br>
 * Maps string -> Object <br>
 * Set through changeFactory.setProperty.
 */
public class GameProperties extends GameDataComponent {
  private static final long serialVersionUID = -1448163357090677564L;

  // keep this in sync with the corresponding property, this name is used by reflection
  public static final String CONSTANT_PROPERTIES_FIELD_NAME = "constantProperties";
  private final Map<String, Object> constantProperties = new HashMap<>();


  // keep this in sync with the corresponding property, this name is used by reflection
  public static final String EDITABLE_PROPERTIES_FIELD_NAME = "editableProperties";
  private final Map<String, IEditableProperty> editableProperties = new HashMap<>();

  // This list is used to keep track of order properties were
  // added.
  private final List<String> ordering = new ArrayList<>();

  private final Map<String, IEditableProperty> playerProperties = new HashMap<>();

  /**
   * Creates a new instance of GameProperties.
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
      constantProperties.remove(key);
      ordering.remove(key);
    } else {
      constantProperties.put(key, value);
      ordering.add(key);
    }
  }

  /**
   * @param key
   *        referring key
   * @return property with key or null if property is not contained in the list
   *         (The object returned should not be modified, as modifications will not appear globally.)
   */
  public Object get(final String key) {
    if (editableProperties.containsKey(key)) {
      return editableProperties.get(key).getValue();
    }
    if (playerProperties.containsKey(key)) {
      return playerProperties.get(key).getValue();
    }
    return constantProperties.get(key);
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

  public void addEditableProperty(final IEditableProperty property) {
    // add to the editable properties
    editableProperties.put(property.getName(), property);
    ordering.add(property.getName());
  }

  /**
   * Return list of editable properties in the order they were added.
   *
   * @return a list of IEditableProperty
   */
  public List<IEditableProperty> getEditableProperties() {
    final List<IEditableProperty> properties = new ArrayList<>();
    for (final String propertyName : ordering) {
      if (editableProperties.containsKey(propertyName)) {
        properties.add(editableProperties.get(propertyName));
      }
    }
    return properties;
  }

  public void addPlayerProperty(final IEditableProperty property) {
    playerProperties.put(property.getName(), property);
  }

  public IEditableProperty getPlayerProperty(final String name) {
    return playerProperties.get(name);
  }

  public static void toOutputStream(final OutputStream sink, final List<IEditableProperty> editableProperties)
      throws IOException {
    // write internally first in case of error
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
        ObjectOutputStream outStream = new ObjectOutputStream(bos);
        GZIPOutputStream zippedOut = new GZIPOutputStream(sink)) {

      outStream.writeObject(editableProperties);
      zippedOut.write(bos.toByteArray());
      zippedOut.flush();
    }
  }

  @SuppressWarnings("unchecked")
  public static List<IEditableProperty> streamToIEditablePropertiesList(final byte[] byteArray)
      throws IOException, ClassNotFoundException, ClassCastException {
    List<IEditableProperty> editableProperties = null;

    try (ByteArrayInputStream byteStream = new ByteArrayInputStream(byteArray);
        InputStream inputStream = new BufferedInputStream(byteStream);
        GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
        ObjectInputStream objectStream = new ObjectInputStream(gzipInputStream)) {

      editableProperties = (List<IEditableProperty>) objectStream.readObject();
    }

    return editableProperties;
  }

  public static void applyByteMapToChangeProperties(final byte[] byteArray,
      final GameProperties gamePropertiesToBeChanged) {
    List<IEditableProperty> editableProperties = null;
    try {
      editableProperties = streamToIEditablePropertiesList(byteArray);
    } catch (final ClassNotFoundException | ClassCastException | IOException e) {
      ClientLogger.logError(
          "An Error occured whilst trying to apply a Byte Map to Property. Bytes: " + Arrays.toString(byteArray), e);
    }
    applyListToChangeProperties(editableProperties, gamePropertiesToBeChanged);
  }

  private static void applyListToChangeProperties(final List<IEditableProperty> editableProperties,
      final GameProperties gamePropertiesToBeChanged) {
    if (editableProperties == null || editableProperties.isEmpty()) {
      return;
    }
    for (final IEditableProperty prop : editableProperties) {
      if (prop == null || prop.getName() == null) {
        continue;
      }
      final IEditableProperty p = gamePropertiesToBeChanged.editableProperties.get(prop.getName());
      if (p != null && prop.getName().equals(p.getName()) && p.validate(prop.getValue())) {
        p.setValue(prop.getValue());
      }
    }
  }
}
