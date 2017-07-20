package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import games.strategy.util.memento.Memento;
import games.strategy.util.memento.PropertyBagMemento;

/**
 * A factory for creating game data mementos for use in tests.
 */
public final class TestGameDataMementoFactory {
  private TestGameDataMementoFactory() {}

  /**
   * Creates a new game data memento that is valid in all respects but includes (or overrides) the specified property.
   *
   * @param name The property name.
   * @param value The property value.
   *
   * @return A new game data memento.
   */
  public static Memento newMementoWithProperty(final String name, final @Nullable Object value) {
    checkNotNull(name);

    final Map<String, Object> propertiesByName = newValidMementoPropertiesByName();
    propertiesByName.put(name, value);
    return newMementoWithProperties(propertiesByName);
  }

  private static Map<String, Object> newValidMementoPropertiesByName() {
    final GameData gameData = TestGameDataFactory.newValidGameData();
    return Maps.newHashMap(ImmutableMap.<String, Object>builder()
        .put(GameDataMemento.PropertyNames.NAME, gameData.getGameName())
        .put(GameDataMemento.PropertyNames.VERSION, gameData.getGameVersion())
        // TODO: handle other properties
        .build());
  }

  private static PropertyBagMemento newMementoWithProperties(final Map<String, Object> propertiesByName) {
    return new PropertyBagMemento(GameDataMemento.SCHEMA_ID, propertiesByName);
  }

  /**
   * Creates a new game data memento that is valid in all respects but does not include the specified property.
   *
   * @param name The property name.
   *
   * @return A new game data memento.
   */
  public static Memento newMementoWithoutProperty(final String name) {
    checkNotNull(name);

    final Map<String, Object> propertiesByName = newValidMementoPropertiesByName();
    propertiesByName.remove(name);
    return newMementoWithProperties(propertiesByName);
  }
}
