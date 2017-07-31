package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.Nullable;

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
   *
   * @throws Exception If the game data memento cannot be created.
   */
  public static Memento newMementoWithProperty(final String name, final @Nullable Object value) throws Exception {
    checkNotNull(name);

    final Map<String, Object> propertiesByName = newValidMementoPropertiesByName();
    propertiesByName.put(name, value);
    return newMementoWithProperties(propertiesByName);
  }

  private static Map<String, Object> newValidMementoPropertiesByName() throws Exception {
    return GameDataMemento.newExporterInternal()
        .exportMemento(TestGameDataFactory.newValidGameData())
        .getPropertiesByName();
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
   *
   * @throws Exception If the game data memento cannot be created.
   */
  public static Memento newMementoWithoutProperty(final String name) throws Exception {
    checkNotNull(name);

    final Map<String, Object> propertiesByName = newValidMementoPropertiesByName();
    propertiesByName.remove(name);
    return newMementoWithProperties(propertiesByName);
  }
}
