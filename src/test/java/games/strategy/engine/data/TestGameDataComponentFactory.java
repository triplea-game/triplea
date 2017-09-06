package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import games.strategy.net.GUID;
import games.strategy.triplea.delegate.FakeTechAdvance;
import games.strategy.util.IntegerMap;

/**
 * A factory for creating game data component instances for use in tests.
 */
public final class TestGameDataComponentFactory {
  private TestGameDataComponentFactory() {}

  /**
   * Creates a new {@link FakeTechAdvance} instance.
   *
   * @param gameData The game data that owns the component.
   * @param name The component name.
   *
   * @return A new {@link FakeTechAdvance} instance.
   */
  public static FakeTechAdvance newFakeTechAdvance(final GameData gameData, final String name) {
    final FakeTechAdvance fakeTechAdvance = new FakeTechAdvance(gameData, name);
    initializeAttachable(fakeTechAdvance);
    return fakeTechAdvance;
  }

  /**
   * Creates a new {@link PlayerID} instance.
   *
   * @param gameData The game data that owns the component.
   * @param name The component name.
   *
   * @return A new {@link PlayerID} instance.
   */
  public static PlayerID newPlayerId(final GameData gameData, final String name) {
    checkNotNull(gameData);
    checkNotNull(name);

    final PlayerID playerId = new PlayerID(name, gameData);
    // TODO: initialize other attributes
    initializeAttachable(playerId);
    return playerId;
  }

  /**
   * Creates a new {@link ProductionRule} instance.
   *
   * @param gameData The game data that owns the component.
   * @param name The component name.
   *
   * @return A new {@link ProductionRule} instance.
   */
  public static ProductionRule newProductionRule(final GameData gameData, final String name) {
    checkNotNull(gameData);
    checkNotNull(name);

    return new ProductionRule(name, gameData, newNamedAttachableIntegerMap(gameData), newResourceIntegerMap(gameData));
  }

  private static IntegerMap<NamedAttachable> newNamedAttachableIntegerMap(final GameData gameData) {
    final IntegerMap<NamedAttachable> integerMap = new IntegerMap<>();
    integerMap.add(newResource(gameData, "resource1"), 11);
    integerMap.add(newResource(gameData, "resource2"), 22);
    return integerMap;
  }

  private static IntegerMap<Resource> newResourceIntegerMap(final GameData gameData) {
    final IntegerMap<Resource> integerMap = new IntegerMap<>();
    integerMap.add(newResource(gameData, "resource3"), 33);
    integerMap.add(newResource(gameData, "resource4"), 44);
    return integerMap;
  }

  /**
   * Creates a new {@link RepairRule} instance.
   *
   * @param gameData The game data that owns the component.
   * @param name The component name.
   *
   * @return A new {@link RepairRule} instance.
   */
  public static RepairRule newRepairRule(final GameData gameData, final String name) {
    checkNotNull(gameData);
    checkNotNull(name);

    return new RepairRule(name, gameData, newNamedAttachableIntegerMap(gameData), newResourceIntegerMap(gameData));
  }

  /**
   * Creates a new {@link Resource} instance.
   *
   * @param gameData The game data that owns the component.
   * @param name The component name.
   *
   * @return A new {@link Resource} instance.
   */
  public static Resource newResource(final GameData gameData, final String name) {
    checkNotNull(gameData);
    checkNotNull(name);

    final Resource resource = new Resource(name, gameData);
    initializeAttachable(resource);
    return resource;
  }

  /**
   * Creates a new {@link TechnologyFrontier} instance.
   *
   * @param gameData The game data that owns the component.
   * @param name The component name.
   *
   * @return A new {@link TechnologyFrontier} instance.
   */
  public static TechnologyFrontier newTechnologyFrontier(final GameData gameData, final String name) {
    final TechnologyFrontier technologyFrontier = new TechnologyFrontier(name, gameData);
    technologyFrontier.addAdvance(Arrays.asList(
        newFakeTechAdvance(gameData, "Tech Advance 1"),
        newFakeTechAdvance(gameData, "Tech Advance 2")));
    return technologyFrontier;
  }

  /**
   * Creates a new {@link Unit} instance.
   *
   * @param gameData The game data that owns the component.
   * @param owner The player that owns the unit.
   * @param typeName The name of the unit type.
   *
   * @return A new {@link Unit} instance.
   */
  public static Unit newUnit(final GameData gameData, final PlayerID owner, final String typeName) {
    checkNotNull(gameData);
    checkNotNull(owner);
    checkNotNull(typeName);

    final Unit unit = new Unit(newUnitType(gameData, typeName), owner, gameData, new GUID());
    unit.setHits(42);
    return unit;
  }

  /**
   * Creates a new {@link UnitType} instance.
   *
   * @param gameData The game data that owns the component.
   * @param name The component name.
   *
   * @return A new {@link UnitType} instance.
   */
  public static UnitType newUnitType(final GameData gameData, final String name) {
    checkNotNull(gameData);
    checkNotNull(name);

    final UnitType unitType = new UnitType(name, gameData);
    initializeAttachable(unitType);
    return unitType;
  }

  /**
   * Initializes the specified {@link Attachable} instance with a set of attachments appropriate for testing.
   *
   * @param attachable The instance to initialize.
   */
  public static void initializeAttachable(final Attachable attachable) {
    checkNotNull(attachable);

    attachable.addAttachment("key1", new FakeAttachment("attachment1"));
    attachable.addAttachment("key2", new FakeAttachment("attachment2"));
  }
}
