package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.Constants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.triplea.java.collections.IntegerMap;

/**
 * Contains some utility methods that subclasses can use to make writing attachments easier. FYI:
 * You may never have a hashmap/linkedhashmap of any other "attachment" within an attachment. This
 * is because there will be a circular reference from this hashmap -> attachment1 -> playerid ->
 * attachment2 -> hashmap -> attachment1, and this causes major problems for Java's deserializing.
 * When deserializing the attachments will not be resolved before their hashcode is taken, resulting
 * in the wrong hashcode and the attachment going in the wrong bucket, so that a .get(attachment1)
 * will result in a null instead of giving the key for attachment1. So just don't have maps of
 * attachments, in an attachment. Thx, Veqryn.
 */
public abstract class DefaultAttachment extends GameDataComponent implements IAttachment {
  private static final long serialVersionUID = -1985116207387301730L;
  private static final Splitter COLON_SPLITTER = Splitter.on(':');

  @Getter @Setter private Attachable attachedTo;
  @Getter private String name;

  protected DefaultAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(gameData);
    setName(name);
    setAttachedTo(attachable);
  }

  /**
   * Gets the attachment with the specified name and type from the specified object.
   *
   * @param namedAttachable The object from which the attachment is to be retrieved.
   * @param attachmentName The name of the attachment to retrieve.
   * @param attachmentType The type of the attachment to retrieve.
   * @return The requested attachment.
   * @throws IllegalStateException If the requested attachment is not found in the specified object.
   * @throws ClassCastException If the requested attachment is not of the specified type.
   */
  protected static <T extends IAttachment> T getAttachment(
      final NamedAttachable namedAttachable,
      final String attachmentName,
      final Class<T> attachmentType) {
    checkNotNull(namedAttachable);
    checkNotNull(attachmentName);
    checkNotNull(attachmentType);
    return Optional.ofNullable(attachmentType.cast(namedAttachable.getAttachment(attachmentName)))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    String.format(
                        "No attachment named '%s' of type '%s' for object named '%s'",
                        attachmentName, attachmentType, namedAttachable.getName())));
  }

  /** Throws an error if format is invalid. */
  protected static int getInt(final String value) {
    try {
      return Integer.parseInt(value);
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException("Attachments: " + value + " is not a valid int value", e);
    }
  }

  /** Throws an error if format is invalid. Must be either true or false ignoring case. */
  protected static boolean getBool(final String value) {
    if (value.equalsIgnoreCase(Constants.PROPERTY_TRUE)) {
      return true;
    } else if (value.equalsIgnoreCase(Constants.PROPERTY_FALSE)) {
      return false;
    }
    throw new IllegalArgumentException("Attachments: " + value + " is not a valid boolean");
  }

  protected static String[] splitOnColon(final String value) {
    checkNotNull(value);

    return Iterables.toArray(COLON_SPLITTER.split(value), String.class);
  }

  protected String thisErrorMsg() {
    return ",   for: " + this;
  }

  /** Returns null or the toString() of the field value. */
  public String getRawPropertyString(final String property) {
    return getProperty(property).map(MutableProperty::getValue).map(Object::toString).orElse(null);
  }

  @Override
  public void setName(final String name) {
    this.name =
        Optional.ofNullable(name)
            // replace-all to automatically correct legacy (1.8) attachment spelling
            .map(attachmentName -> attachmentName.replaceAll("ttatch", "ttach"))
            .orElse(null);
  }

  /**
   * Any overriding method for toString needs to include at least the Class, attachedTo, and name.
   * Or call super.toString()
   */
  @Override
  public String toString() {
    return getClass().getSimpleName() + " attached to: " + attachedTo + " with name: " + name;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(attachedTo, name);
  }

  @Override
  public final boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof DefaultAttachment)) {
      return false;
    }

    final DefaultAttachment other = (DefaultAttachment) obj;
    return getClass().equals(other.getClass())
        && Objects.equals(
            Objects.toString(attachedTo, null), Objects.toString(other.attachedTo, null))
        && (Objects.equals(name, other.name) || this.toString().equals(other.toString()));
  }

  protected Territory getTerritoryOrThrow(String name) throws GameParseException {
    return Optional.ofNullable(getData().getMap().getTerritory(name))
        .orElseThrow(() -> new GameParseException("No territory named: " + name + thisErrorMsg()));
  }

  protected List<GamePlayer> parsePlayerList(final String value, List<GamePlayer> existingList)
      throws GameParseException {
    for (final String name : splitOnColon(value)) {
      if (existingList == null) {
        existingList = new ArrayList<>();
      }
      existingList.add(getPlayerOrThrow(name));
    }
    return existingList;
  }

  protected GamePlayer getPlayerOrThrow(String name) throws GameParseException {
    return Optional.ofNullable(getData().getPlayerList().getPlayerId(name))
        .orElseThrow(() -> new GameParseException("No player named: " + name + thisErrorMsg()));
  }

  protected Set<UnitType> parseUnitTypes(String context, String value, Set<UnitType> existingSet)
      throws GameParseException {
    for (final String u : splitOnColon(value)) {
      if (existingSet == null) {
        existingSet = new HashSet<>();
      }
      existingSet.add(getUnitTypeOrThrow(u));
    }
    return existingSet;
  }

  public UnitType getUnitTypeOrThrow(String unitType) throws GameParseException {
    return Optional.ofNullable(getData().getUnitTypeList().getUnitType(unitType))
        .orElseThrow(() -> new GameParseException("No unit type: " + unitType + thisErrorMsg()));
  }

  protected static <T> List<T> getListProperty(@Nullable List<T> value) {
    if (value == null) {
      return List.of();
    }
    return Collections.unmodifiableList(value);
  }

  protected static <K, V> Map<K, V> getMapProperty(@Nullable Map<K, V> value) {
    if (value == null) {
      return Map.of();
    }
    return Collections.unmodifiableMap(value);
  }

  protected static <T> Set<T> getSetProperty(@Nullable Set<T> value) {
    if (value == null) {
      return Set.of();
    }
    return Collections.unmodifiableSet(value);
  }

  protected static <T> IntegerMap<T> getIntegerMapProperty(@Nullable IntegerMap<T> value) {
    if (value == null) {
      return IntegerMap.of();
    }
    return IntegerMap.unmodifiableViewOf(value);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static Object copyPropertyValue(Object value) {
    if (value instanceof List) {
      return new ArrayList((List) value);
    } else if (value instanceof IntegerMap) {
      return new IntegerMap((IntegerMap) value);
    } else if (value instanceof Set) {
      return new HashSet((Set) value);
    } else if (value instanceof Map) {
      return new HashMap((Map) value);
    }
    return value;
  }
}
