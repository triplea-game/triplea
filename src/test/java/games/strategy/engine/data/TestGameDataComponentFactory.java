package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.triplea.delegate.AARadarAdvance;
import games.strategy.triplea.delegate.DestroyerBombardTechAdvance;
import games.strategy.triplea.delegate.HeavyBomberAdvance;
import games.strategy.util.IntegerMap;

/**
 * A factory for creating game data component instances for use in tests.
 */
public final class TestGameDataComponentFactory {
  private TestGameDataComponentFactory() {}

  /**
   * Creates a new {@link AARadarAdvance} instance.
   *
   * @param gameData The game data associated with the advance.
   *
   * @return A new {@link AARadarAdvance} instance.
   */
  public static AARadarAdvance newAaRadarAdvance(final GameData gameData) {
    checkNotNull(gameData);

    final AARadarAdvance aaRadarAdvance = new AARadarAdvance(gameData);
    aaRadarAdvance.addAttachment("key1", new FakeAttachment("attachment1"));
    aaRadarAdvance.addAttachment("key2", new FakeAttachment("attachment2"));
    return aaRadarAdvance;
  }

  /**
   * Creates a new {@link DestroyerBombardTechAdvance} instance.
   *
   * @param gameData The game data associated with the advance.
   *
   * @return A new {@link DestroyerBombardTechAdvance} instance.
   */
  public static DestroyerBombardTechAdvance newDestroyerBombardTechAdvance(final GameData gameData) {
    checkNotNull(gameData);

    final DestroyerBombardTechAdvance destroyerBombardTechAdvance = new DestroyerBombardTechAdvance(gameData);
    destroyerBombardTechAdvance.addAttachment("key1", new FakeAttachment("attachment1"));
    destroyerBombardTechAdvance.addAttachment("key2", new FakeAttachment("attachment2"));
    return destroyerBombardTechAdvance;
  }

  /**
   * Creates a new {@link HeavyBomberAdvance} instance.
   *
   * @param gameData The game data associated with the advance.
   *
   * @return A new {@link HeavyBomberAdvance} instance.
   */
  public static HeavyBomberAdvance newHeavyBomberAdvance(final GameData gameData) {
    checkNotNull(gameData);

    final HeavyBomberAdvance heavyBomberAdvance = new HeavyBomberAdvance(gameData);
    heavyBomberAdvance.addAttachment("key1", new FakeAttachment("attachment1"));
    heavyBomberAdvance.addAttachment("key2", new FakeAttachment("attachment2"));
    return heavyBomberAdvance;
  }

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

    final IntegerMap<NamedAttachable> results = new IntegerMap<>();
    results.add(newResource(gameData, "resource1"), 11);
    results.add(newResource(gameData, "resource2"), 22);
    final IntegerMap<Resource> costs = new IntegerMap<>();
    costs.add(newResource(gameData, "resource3"), 33);
    costs.add(newResource(gameData, "resource4"), 44);
    return new ProductionRule(name, gameData, results, costs);
  }

  /**
   * Creates a new {@link RepairRule} instance.
   *
   * @param gameData The game data associated with the repair rule.
   * @param name The repair rule name.
   *
   * @return A new {@link RepairRule} instance.
   */
  public static RepairRule newRepairRule(final GameData gameData, final String name) {
    checkNotNull(gameData);
    checkNotNull(name);

    final IntegerMap<NamedAttachable> results = new IntegerMap<>();
    results.add(newResource(gameData, "resource1"), 11);
    results.add(newResource(gameData, "resource2"), 22);
    final IntegerMap<Resource> costs = new IntegerMap<>();
    costs.add(newResource(gameData, "resource3"), 33);
    costs.add(newResource(gameData, "resource4"), 44);
    return new RepairRule("repairRule", gameData, results, costs);
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
