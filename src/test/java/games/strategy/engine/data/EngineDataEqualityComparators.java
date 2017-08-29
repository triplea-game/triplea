package games.strategy.engine.data;

import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.AARadarAdvance;
import games.strategy.triplea.delegate.DestroyerBombardTechAdvance;
import games.strategy.triplea.delegate.HeavyBomberAdvance;

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
}
