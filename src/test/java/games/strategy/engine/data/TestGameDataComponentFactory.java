package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.util.IntegerMap;

/**
 * A factory for creating game data component instances for use in tests.
 */
public final class TestGameDataComponentFactory {
  private TestGameDataComponentFactory() {}

  /**
   * Creates a new {@link ProductionRule} instance.
   *
   * @param gameData The game data associated with the production rule.
   * @param name The production rule name.
   *
   * @return A new {@link ProductionRule} instance.
   */
  public static ProductionRule newProductionRule(final GameData gameData, final String name) {
    checkNotNull(gameData);
    checkNotNull(name);

    final IntegerMap<NamedAttachable> resources = new IntegerMap<>();
    resources.add(newResource(gameData, "resource1"), 11);
    resources.add(newResource(gameData, "resource2"), 22);
    final IntegerMap<Resource> costs = new IntegerMap<>();
    costs.add(newResource(gameData, "resource3"), 33);
    costs.add(newResource(gameData, "resource4"), 44);
    return new ProductionRule(name, gameData, resources, costs);
  }

  /**
   * Creates a new {@link Resource} instance.
   *
   * @param gameData The game data associated with the resource.
   * @param name The resource name.
   *
   * @return A new {@link Resource} instance.
   */
  public static Resource newResource(final GameData gameData, final String name) {
    checkNotNull(gameData);
    checkNotNull(name);

    final Resource resource = new Resource(name, gameData);
    resource.addAttachment("key1", new FakeAttachment("attachment1"));
    resource.addAttachment("key2", new FakeAttachment("attachment2"));
    return resource;
  }
}
