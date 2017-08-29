package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.triplea.delegate.AARadarAdvance;
import games.strategy.triplea.delegate.DestroyerBombardTechAdvance;
import games.strategy.triplea.delegate.HeavyBomberAdvance;
import games.strategy.triplea.delegate.ImprovedArtillerySupportAdvance;
import games.strategy.triplea.delegate.ImprovedShipyardsAdvance;
import games.strategy.triplea.delegate.IncreasedFactoryProductionAdvance;
import games.strategy.triplea.delegate.IndustrialTechnologyAdvance;
import games.strategy.triplea.delegate.JetPowerAdvance;
import games.strategy.triplea.delegate.LongRangeAircraftAdvance;
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
   * Creates a new {@link ImprovedArtillerySupportAdvance} instance.
   *
   * @param gameData The game data associated with the advance.
   *
   * @return A new {@link ImprovedArtillerySupportAdvance} instance.
   */
  public static ImprovedArtillerySupportAdvance newImprovedArtillerySupportAdvance(final GameData gameData) {
    checkNotNull(gameData);

    final ImprovedArtillerySupportAdvance improvedArtillerySupportAdvance =
        new ImprovedArtillerySupportAdvance(gameData);
    improvedArtillerySupportAdvance.addAttachment("key1", new FakeAttachment("attachment1"));
    improvedArtillerySupportAdvance.addAttachment("key2", new FakeAttachment("attachment2"));
    return improvedArtillerySupportAdvance;
  }

  /**
   * Creates a new {@link ImprovedShipyardsAdvance} instance.
   *
   * @param gameData The game data associated with the advance.
   *
   * @return A new {@link ImprovedShipyardsAdvance} instance.
   */
  public static ImprovedShipyardsAdvance newImprovedShipyardsAdvance(final GameData gameData) {
    checkNotNull(gameData);

    final ImprovedShipyardsAdvance improvedShipyardsAdvance = new ImprovedShipyardsAdvance(gameData);
    improvedShipyardsAdvance.addAttachment("key1", new FakeAttachment("attachment1"));
    improvedShipyardsAdvance.addAttachment("key2", new FakeAttachment("attachment2"));
    return improvedShipyardsAdvance;
  }

  /**
   * Creates a new {@link IncreasedFactoryProductionAdvance} instance.
   *
   * @param gameData The game data associated with the advance.
   *
   * @return A new {@link IncreasedFactoryProductionAdvance} instance.
   */
  public static IncreasedFactoryProductionAdvance newIncreasedFactoryProductionAdvance(final GameData gameData) {
    checkNotNull(gameData);

    final IncreasedFactoryProductionAdvance increasedFactoryProductionAdvance =
        new IncreasedFactoryProductionAdvance(gameData);
    increasedFactoryProductionAdvance.addAttachment("key1", new FakeAttachment("attachment1"));
    increasedFactoryProductionAdvance.addAttachment("key2", new FakeAttachment("attachment2"));
    return increasedFactoryProductionAdvance;
  }

  /**
   * Creates a new {@link IndustrialTechnologyAdvance} instance.
   *
   * @param gameData The game data associated with the advance.
   *
   * @return A new {@link IndustrialTechnologyAdvance} instance.
   */
  public static IndustrialTechnologyAdvance newIndustrialTechnologyAdvance(final GameData gameData) {
    checkNotNull(gameData);

    final IndustrialTechnologyAdvance industrialTechnologyAdvance = new IndustrialTechnologyAdvance(gameData);
    industrialTechnologyAdvance.addAttachment("key1", new FakeAttachment("attachment1"));
    industrialTechnologyAdvance.addAttachment("key2", new FakeAttachment("attachment2"));
    return industrialTechnologyAdvance;
  }

  /**
   * Creates a new {@link JetPowerAdvance} instance.
   *
   * @param gameData The game data associated with the advance.
   *
   * @return A new {@link JetPowerAdvance} instance.
   */
  public static JetPowerAdvance newJetPowerAdvance(final GameData gameData) {
    checkNotNull(gameData);

    final JetPowerAdvance jetPowerAdvance = new JetPowerAdvance(gameData);
    jetPowerAdvance.addAttachment("key1", new FakeAttachment("attachment1"));
    jetPowerAdvance.addAttachment("key2", new FakeAttachment("attachment2"));
    return jetPowerAdvance;
  }

  /**
   * Creates a new {@link LongRangeAircraftAdvance} instance.
   *
   * @param gameData The game data associated with the advance.
   *
   * @return A new {@link LongRangeAircraftAdvance} instance.
   */
  public static LongRangeAircraftAdvance newLongRangeAircraftAdvance(final GameData gameData) {
    checkNotNull(gameData);

    final LongRangeAircraftAdvance longRangeAircraftAdvance = new LongRangeAircraftAdvance(gameData);
    longRangeAircraftAdvance.addAttachment("key1", new FakeAttachment("attachment1"));
    longRangeAircraftAdvance.addAttachment("key2", new FakeAttachment("attachment2"));
    return longRangeAircraftAdvance;
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
