package games.strategy.engine.data;

import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.AARadarAdvance;
import games.strategy.triplea.delegate.DestroyerBombardTechAdvance;
import games.strategy.triplea.delegate.FakeTechAdvance;
import games.strategy.triplea.delegate.HeavyBomberAdvance;
import games.strategy.triplea.delegate.ImprovedArtillerySupportAdvance;
import games.strategy.triplea.delegate.ImprovedShipyardsAdvance;
import games.strategy.triplea.delegate.IncreasedFactoryProductionAdvance;
import games.strategy.triplea.delegate.IndustrialTechnologyAdvance;
import games.strategy.triplea.delegate.JetPowerAdvance;
import games.strategy.triplea.delegate.LongRangeAircraftAdvance;
import games.strategy.triplea.delegate.MechanizedInfantryAdvance;
import games.strategy.triplea.delegate.ParatroopersAdvance;
import games.strategy.triplea.delegate.RocketsAdvance;
import games.strategy.triplea.delegate.SuperSubsAdvance;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.WarBondsAdvance;

/**
 * A collection of equality comparators for engine data types.
 */
public final class EngineDataEqualityComparators {
  private EngineDataEqualityComparators() {}

  public static final EqualityComparator AA_RADAR_ADVANCE = EqualityComparator.newInstance(
      AARadarAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator DESTROYER_BOMBARD_TECH_ADVANCE = EqualityComparator.newInstance(
      DestroyerBombardTechAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator FAKE_TECH_ADVANCE = EqualityComparator.newInstance(
      FakeTechAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator GAME_DATA = EqualityComparator.newInstance(
      GameData.class,
      (context, o1, o2) -> (o1.getDiceSides() == o2.getDiceSides())
          && areBothNullOrBothNotNull(o1.getGameLoader(), o2.getGameLoader())
          && context.equals(o1.getGameName(), o2.getGameName())
          && context.equals(o1.getGameVersion(), o2.getGameVersion()));

  private static boolean areBothNullOrBothNotNull(final Object o1, final Object o2) {
    return (o1 == null) == (o2 == null);
  }

  public static final EqualityComparator HEAVY_BOMBER_ADVANCE = EqualityComparator.newInstance(
      HeavyBomberAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator IMPROVED_ARTILLERY_SUPPORT_ADVANCE = EqualityComparator.newInstance(
      ImprovedArtillerySupportAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator IMPROVED_SHIPYARDS_ADVANCE = EqualityComparator.newInstance(
      ImprovedShipyardsAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator INCREASED_FACTORY_PRODUCTION_ADVANCE = EqualityComparator.newInstance(
      IncreasedFactoryProductionAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator INDUSTRIAL_TECHNOLOGY_ADVANCE = EqualityComparator.newInstance(
      IndustrialTechnologyAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator JET_POWER_ADVANCE = EqualityComparator.newInstance(
      JetPowerAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator LONG_RANGE_AIRCRAFT_ADVANCE = EqualityComparator.newInstance(
      LongRangeAircraftAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator MECHANIZED_INFANTRY_ADVANCE = EqualityComparator.newInstance(
      MechanizedInfantryAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator PARATROOPERS_ADVANCE = EqualityComparator.newInstance(
      ParatroopersAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator PLAYER_ID = EqualityComparator.newInstance(
      PlayerID.class,
      // TODO: add comparisons for other attributes
      EngineDataEqualityComparators::namedAttachableEquals);

  public static final EqualityComparator PRODUCTION_FRONTIER = EqualityComparator.newInstance(
      ProductionFrontier.class,
      (context, o1, o2) -> defaultNamedEquals(context, o1, o2)
          && context.equals(o1.getRules(), o2.getRules()));

  public static final EqualityComparator PRODUCTION_RULE = EqualityComparator.newInstance(
      ProductionRule.class,
      (context, o1, o2) -> defaultNamedEquals(context, o1, o2)
          && context.equals(o1.getCosts(), o2.getCosts())
          && context.equals(o1.getResults(), o2.getResults()));

  public static final EqualityComparator REPAIR_FRONTIER = EqualityComparator.newInstance(
      RepairFrontier.class,
      (context, o1, o2) -> defaultNamedEquals(context, o1, o2)
          && context.equals(o1.getRules(), o2.getRules()));

  public static final EqualityComparator REPAIR_RULE = EqualityComparator.newInstance(
      RepairRule.class,
      (context, o1, o2) -> defaultNamedEquals(context, o1, o2)
          && context.equals(o1.getCosts(), o2.getCosts())
          && context.equals(o1.getResults(), o2.getResults()));

  public static final EqualityComparator RESOURCE = EqualityComparator.newInstance(
      Resource.class,
      EngineDataEqualityComparators::namedAttachableEquals);

  public static final EqualityComparator RESOURCE_COLLECTION = EqualityComparator.newInstance(
      ResourceCollection.class,
      (context, o1, o2) -> gameDataComponentEquals(context, o1, o2)
          && context.equals(o1.getResourcesCopy(), o2.getResourcesCopy()));

  public static final EqualityComparator ROCKETS_ADVANCE = EqualityComparator.newInstance(
      RocketsAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator SUPER_SUBS_ADVANCE = EqualityComparator.newInstance(
      SuperSubsAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  public static final EqualityComparator TECHNOLOGY_FRONTIER = EqualityComparator.newInstance(
      TechnologyFrontier.class,
      (context, o1, o2) -> gameDataComponentEquals(context, o1, o2)
          && context.equals(o1.getName(), o2.getName())
          && context.equals(o1.getTechs(), o2.getTechs()));

  public static final EqualityComparator TECHNOLOGY_FRONTIER_LIST = EqualityComparator.newInstance(
      TechnologyFrontierList.class,
      (context, o1, o2) -> gameDataComponentEquals(context, o1, o2)
          && context.equals(o1.getFrontiers(), o2.getFrontiers()));

  public static final EqualityComparator UNIT_TYPE = EqualityComparator.newInstance(
      UnitType.class,
      EngineDataEqualityComparators::namedAttachableEquals);

  public static final EqualityComparator WAR_BONDS_ADVANCE = EqualityComparator.newInstance(
      WarBondsAdvance.class,
      EngineDataEqualityComparators::techAdvanceEquals);

  private static boolean techAdvanceEquals(
      final EqualityComparator.Context context,
      final TechAdvance o1,
      final TechAdvance o2) {
    return namedAttachableEquals(context, o1, o2);
  }

  private static boolean namedAttachableEquals(
      final EqualityComparator.Context context,
      final NamedAttachable o1,
      final NamedAttachable o2) {
    return defaultNamedEquals(context, o1, o2)
        && attachableEquals(context, o1, o2);
  }

  private static boolean defaultNamedEquals(
      final EqualityComparator.Context context,
      final DefaultNamed o1,
      final DefaultNamed o2) {
    return gameDataComponentEquals(context, o1, o2)
        && namedEquals(context, o1, o2);
  }

  private static boolean gameDataComponentEquals(
      final EqualityComparator.Context context,
      final GameDataComponent o1,
      final GameDataComponent o2) {
    return context.equals(o1.getData(), o2.getData());
  }

  private static boolean namedEquals(
      final EqualityComparator.Context context,
      final Named o1,
      final Named o2) {
    return context.equals(o1.getName(), o2.getName());
  }

  private static boolean attachableEquals(
      final EqualityComparator.Context context,
      final Attachable o1,
      final Attachable o2) {
    return context.equals(o1.getAttachments(), o2.getAttachments());
  }
}
