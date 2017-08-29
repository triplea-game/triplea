package games.strategy.engine.data;

import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.AARadarAdvance;
import games.strategy.triplea.delegate.DestroyerBombardTechAdvance;
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
import games.strategy.triplea.delegate.WarBondsAdvance;

/**
 * A collection of equality comparators for engine data types.
 */
public final class EngineDataEqualityComparators {
  private EngineDataEqualityComparators() {}

  public static final EqualityComparator AA_RADAR_ADVANCE = EqualityComparator.newInstance(
      AARadarAdvance.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

  public static final EqualityComparator DESTROYER_BOMBARD_TECH_ADVANCE = EqualityComparator.newInstance(
      DestroyerBombardTechAdvance.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

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
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

  public static final EqualityComparator IMPROVED_ARTILLERY_SUPPORT_ADVANCE = EqualityComparator.newInstance(
      ImprovedArtillerySupportAdvance.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

  public static final EqualityComparator IMPROVED_SHIPYARDS_ADVANCE = EqualityComparator.newInstance(
      ImprovedShipyardsAdvance.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

  public static final EqualityComparator INCREASED_FACTORY_PRODUCTION_ADVANCE = EqualityComparator.newInstance(
      IncreasedFactoryProductionAdvance.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

  public static final EqualityComparator INDUSTRIAL_TECHNOLOGY_ADVANCE = EqualityComparator.newInstance(
      IndustrialTechnologyAdvance.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

  public static final EqualityComparator JET_POWER_ADVANCE = EqualityComparator.newInstance(
      JetPowerAdvance.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

  public static final EqualityComparator LONG_RANGE_AIRCRAFT_ADVANCE = EqualityComparator.newInstance(
      LongRangeAircraftAdvance.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

  public static final EqualityComparator MECHANIZED_INFANTRY_ADVANCE = EqualityComparator.newInstance(
      MechanizedInfantryAdvance.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

  public static final EqualityComparator PARATROOPERS_ADVANCE = EqualityComparator.newInstance(
      ParatroopersAdvance.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

  public static final EqualityComparator PRODUCTION_FRONTIER = EqualityComparator.newInstance(
      ProductionFrontier.class,
      (context, o1, o2) -> context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName())
          && context.equals(o1.getRules(), o2.getRules()));

  public static final EqualityComparator PRODUCTION_RULE = EqualityComparator.newInstance(
      ProductionRule.class,
      (context, o1, o2) -> context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName())
          && context.equals(o1.getCosts(), o2.getCosts())
          && context.equals(o1.getResults(), o2.getResults()));

  public static final EqualityComparator REPAIR_FRONTIER = EqualityComparator.newInstance(
      RepairFrontier.class,
      (context, o1, o2) -> context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName())
          && context.equals(o1.getRules(), o2.getRules()));

  public static final EqualityComparator REPAIR_RULE = EqualityComparator.newInstance(
      RepairRule.class,
      (context, o1, o2) -> context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName())
          && context.equals(o1.getCosts(), o2.getCosts())
          && context.equals(o1.getResults(), o2.getResults()));

  public static final EqualityComparator RESOURCE = EqualityComparator.newInstance(
      Resource.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

  public static final EqualityComparator RESOURCE_COLLECTION = EqualityComparator.newInstance(
      ResourceCollection.class,
      (context, o1, o2) -> context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getResourcesCopy(), o2.getResourcesCopy()));

  public static final EqualityComparator ROCKETS_ADVANCE = EqualityComparator.newInstance(
      RocketsAdvance.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

  public static final EqualityComparator SUPER_SUBS_ADVANCE = EqualityComparator.newInstance(
      SuperSubsAdvance.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));

  public static final EqualityComparator WAR_BONDS_ADVANCE = EqualityComparator.newInstance(
      WarBondsAdvance.class,
      (context, o1, o2) -> context.equals(o1.getAttachments(), o2.getAttachments())
          && context.equals(o1.getData(), o2.getData())
          && context.equals(o1.getName(), o2.getName()));
}
