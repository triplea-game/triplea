package games.strategy.engine.data.properties;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataComponent;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.Constants;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.triplea.io.IoUtils;

/**
 * Properties of the current game. <br>
 * Maps string -> Object <br>
 * Set through changeFactory.setProperty.
 */
@Slf4j
public class GameProperties extends GameDataComponent {
  @Serial private static final long serialVersionUID = -1448163357090677564L;

  private final Map<String, Serializable> constantProperties = new HashMap<>();
  private final Map<String, IEditableProperty<?>> editableProperties = new HashMap<>();
  // This list is used to keep track of order properties were added.
  private final List<String> ordering = new ArrayList<>();
  private final Map<String, IEditableProperty<?>> playerProperties = new HashMap<>();

  public GameProperties(final GameData data) {
    super(data);
  }

  public Map<String, Object> getConstantPropertiesByName() {
    return new HashMap<>(constantProperties);
  }

  public Map<String, IEditableProperty<?>> getEditablePropertiesByName() {
    return new HashMap<>(editableProperties);
  }

  /**
   * Sets a serializable {@code value} for a specified {@code key}. Setting a property to {@code
   * null} has the effect of unbinding the key.
   *
   * @param key key of property
   * @param value property
   */
  public void set(final String key, final @Nullable Serializable value) {
    if (value == null) {
      constantProperties.remove(key);
      ordering.remove(key);
    } else {
      constantProperties.put(key, value);
      ordering.add(key);
    }
  }

  public void set(final String key, final @Nullable Object value) {
    set(key, (Serializable) value);
  }

  /**
   * Ensures that the properties contain the one for {@code propertyKey}
   *
   * @param propertyKey property key
   * @return property according to {@link #getPlayerProperty(String)} or throws ans
   */
  public IEditableProperty<?> getPlayerPropertyOrThrow(final String propertyKey) {
    final Optional<IEditableProperty<?>> optionalPlayerProperty = getPlayerProperty(propertyKey);
    return optionalPlayerProperty.orElseThrow(
        () ->
            new IllegalArgumentException(
                MessageFormat.format("Property not found: {0}", propertyKey)));
  }

  /**
   * Ensures that the properties contain the one for PU Income Bonus for a {@code gamePlayer}
   *
   * @param gamePlayer {@code GamePlayer} of the property
   * @return property according to {@link Constants#getPropertyNamePuIncomeBonusFor(GamePlayer)} or
   *     default
   */
  public IEditableProperty<?> ensurePropertyPuIncomeBonusFor(final GamePlayer gamePlayer) {
    final String propertyKey = Constants.getPropertyNamePuIncomeBonusFor(gamePlayer);
    final Optional<IEditableProperty<?>> optionalNewPlayerPropertyPuIncomeBonus =
        getPlayerProperty(propertyKey);
    if (optionalNewPlayerPropertyPuIncomeBonus.isEmpty()) {
      final String oldPropertyKey =
          MessageFormat.format("{0}PU Income Bonus", gamePlayer.getName());
      final Optional<IEditableProperty<?>> optionalOldPlayerPropertyPuIncomeBonus =
          getPlayerProperty(oldPropertyKey);
      final NumberProperty newProperty =
          ((NumberProperty)
                  optionalOldPlayerPropertyPuIncomeBonus.orElseThrow(
                      () ->
                          new IllegalArgumentException(
                              MessageFormat.format(
                                  "Property not found: {0} or {1}", propertyKey, oldPropertyKey))))
              .cloneAs(propertyKey);
      addPlayerProperty(newProperty);
      set(oldPropertyKey, null);
      return newProperty;
    }
    return optionalNewPlayerPropertyPuIncomeBonus.get();
  }

  /**
   * Returns property with key or null if property is not contained in the list. (The object
   * returned should not be modified, as modifications will not appear globally.)
   *
   * @param key referring key
   */
  public Serializable get(final String key) {
    IEditableProperty found = editableProperties.get(key);
    if (found != null) {
      return (Serializable) found.getValue();
    }
    found = playerProperties.get(key);
    if (found != null) {
      return (Serializable) found.getValue();
    }
    return constantProperties.get(key);
  }

  public boolean get(final String key, final boolean defaultValue) {
    final Object value = get(key);
    if (value == null) {
      return defaultValue;
    }
    return (boolean) value;
  }

  public int get(final String key, final int defaultValue) {
    final Object value = get(key);
    if (value == null) {
      return defaultValue;
    }
    return (int) value;
  }

  public String get(final String key, final String defaultValue) {
    final Object value = get(key);
    if (value == null) {
      return defaultValue;
    }
    return String.valueOf(value);
  }

  public void addEditableProperty(final IEditableProperty<?> property) {
    // add to the editable properties
    editableProperties.put(property.getName(), property);
    ordering.add(property.getName());
  }

  /**
   * Return list of editable properties in the order they were added.
   *
   * @return a list of IEditableProperty
   */
  public List<IEditableProperty<?>> getEditableProperties() {
    final List<IEditableProperty<?>> properties = new ArrayList<>();
    for (final String propertyName : ordering) {
      if (editableProperties.containsKey(propertyName)) {
        properties.add(editableProperties.get(propertyName));
      }
    }
    return properties;
  }

  public void addPlayerProperty(final IEditableProperty<?> property) {
    playerProperties.put(property.getName(), property);
  }

  public Optional<IEditableProperty<?>> getPlayerProperty(final String name) {
    return Optional.ofNullable(playerProperties.get(name));
  }

  /**
   * Writes the specified list of editable properties to a byte array.
   *
   * @param editableProperties The list of editable properties to write.
   * @return A byte array containing the list of editable properties.
   * @throws IOException If an I/O error occurs while writing the list of editable properties.
   */
  public static byte[] writeEditableProperties(
      final List<? extends IEditableProperty<?>> editableProperties) throws IOException {
    return IoUtils.writeToMemory(
        os -> {
          try (var gzipOutputStream = new GZIPOutputStream(os);
              var objectOutputStream = new ObjectOutputStream(gzipOutputStream)) {
            objectOutputStream.writeObject(editableProperties);
          }
        });
  }

  /**
   * Reads a list of editable properties from the specified byte array.
   *
   * @param bytes The byte array containing the list of editable properties.
   * @return The list of editable properties read from the specified byte array.
   * @throws IOException If an I/O error occurs while reading the list of editable properties.
   * @throws ClassCastException If {@code byteArray} contains an object other than a list of
   *     editable properties.
   */
  @SuppressWarnings("unchecked")
  public static List<IEditableProperty<?>> readEditableProperties(final byte[] bytes)
      throws IOException, ClassCastException {
    return IoUtils.readFromMemory(
        bytes,
        is -> {
          try (InputStream bis = new BufferedInputStream(is);
              InputStream gzipis = new GZIPInputStream(bis);
              ObjectInputStream ois = new ObjectInputStream(gzipis)) {
            return (List<IEditableProperty<?>>) ois.readObject();
          } catch (final ClassNotFoundException e) {
            throw new IOException(e);
          }
        });
  }

  /** Updates 'gamePropertiesToBeChanged' with editableProperties. */
  public static void applyByteMapToChangeProperties(
      final byte[] byteArray, final GameProperties gamePropertiesToBeChanged) {
    List<IEditableProperty<?>> editableProperties = null;
    try {
      editableProperties = readEditableProperties(byteArray);
    } catch (final ClassCastException | IOException e) {
      log.error(
          "An Error occurred whilst trying to apply a Byte Map to Property. Bytes: "
              + Arrays.toString(byteArray),
          e);
    }
    applyListToChangeProperties(editableProperties, gamePropertiesToBeChanged);
  }

  private static void applyListToChangeProperties(
      final List<IEditableProperty<?>> editableProperties,
      final GameProperties gamePropertiesToBeChanged) {
    if (editableProperties == null || editableProperties.isEmpty()) {
      return;
    }
    for (final IEditableProperty<?> prop : editableProperties) {
      if (prop == null || prop.getName() == null) {
        continue;
      }
      final IEditableProperty<?> p =
          gamePropertiesToBeChanged.editableProperties.get(prop.getName());
      if (p != null && prop.getName().equals(p.getName())) {
        p.setValueIfValid(prop.getValue());
      }
    }
  }
}
