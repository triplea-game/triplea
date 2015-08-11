package games.strategy.triplea.ai.Dynamix_AI.CommandCenter;

import java.util.HashMap;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ai.Dynamix_AI.DMatches;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
import games.strategy.triplea.delegate.Matches;

@SuppressWarnings({"unchecked", "rawtypes"})
public class CachedCalculationCenter {
  public static HashMap<Territory, List<Territory>> CachedMapTersFromPoints = new HashMap<Territory, List<Territory>>();
  public static HashMap<List<Territory>, Route> CachedRoutes = new HashMap<List<Territory>, Route>();
  public static HashMap<List<Territory>, Route> CachedAirPassableRoutes = new HashMap<List<Territory>, Route>();
  public static HashMap<List<Territory>, Route> CachedLandRoutes = new HashMap<List<Territory>, Route>();
  public static HashMap<List<Territory>, Route> CachedPassableLandRoutes = new HashMap<List<Territory>, Route>();
  public static HashMap<List<Territory>, Route> CachedSeaRoutes = new HashMap<List<Territory>, Route>();

  public static void clearCachedStaticData() {
    CachedMapTersFromPoints = new HashMap<Territory, List<Territory>>();
    CachedRoutes = new HashMap<List<Territory>, Route>();
    CachedAirPassableRoutes = new HashMap<List<Territory>, Route>();
    CachedLandRoutes = new HashMap<List<Territory>, Route>();
    CachedPassableLandRoutes = new HashMap<List<Territory>, Route>();
    CachedSeaRoutes = new HashMap<List<Territory>, Route>();
  }

  /**
   * The same as data.getMap().getRoute(ter1, ter2), except that this method caches the resulting List<Territory> for
   * quick retrieval later
   * on.
   */
  public static List<Territory> GetMapTersFromPoint(final Territory target) {
    final Territory key = target;
    if (!CachedMapTersFromPoints.containsKey(key)) {
      CachedMapTersFromPoints.put(key,
          DUtils.GetTerritoriesWithinXDistanceOfY(target.getData(), target, Integer.MAX_VALUE));
    }
    return CachedMapTersFromPoints.get(key);
  }

  /**
   * The same as data.getMap().getRoute(ter1, ter2), except that this method caches the resulting Route for quick
   * retrieval later on.
   */
  public static Route GetRoute(final GameData data, final Territory ter1, final Territory ter2) {
    final List key = DUtils.ToList(DUtils.ToArray(ter1, ter2));
    if (!CachedRoutes.containsKey(key)) {
      CachedRoutes.put(key, data.getMap().getRoute(ter1, ter2));
    }
    return CachedRoutes.get(key);
  }

  /**
   * The same as data.getMap().getRoute(ter1, ter2, Matches.TerritoryIsNotImpassable), except that this method caches
   * the resulting Route
   * for quick retrieval later on.
   */
  public static Route GetAirPassableRoute(final GameData data, final Territory ter1, final Territory ter2) {
    final List key = DUtils.ToList(DUtils.ToArray(ter1, ter2));
    if (!CachedAirPassableRoutes.containsKey(key)) {
      CachedAirPassableRoutes.put(key, data.getMap().getRoute(ter1, ter2, Matches.TerritoryIsNotImpassable));
    }
    return CachedAirPassableRoutes.get(key);
  }

  /**
   * The same as data.getMap().getLandRoute(ter1, ter2), except that this method caches the resulting Route for quick
   * retrieval later on.
   */
  public static Route GetLandRoute(final GameData data, final Territory ter1, final Territory ter2) {
    final List key = DUtils.ToList(DUtils.ToArray(ter1, ter2));
    if (!CachedLandRoutes.containsKey(key)) {
      CachedLandRoutes.put(key, data.getMap().getLandRoute(ter1, ter2));
    }
    return CachedLandRoutes.get(key);
  }

  /**
   * The same as data.getMap().getRoute(ter1, ter2, new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand,
   * Matches.TerritoryIsNotImpassable)), except that this method caches the resulting Route for quick retrieval later
   * on.
   */
  public static Route GetPassableLandRoute(final GameData data, final Territory ter1, final Territory ter2) {
    final List key = DUtils.ToList(DUtils.ToArray(ter1, ter2));
    if (!CachedPassableLandRoutes.containsKey(key)) {
      CachedPassableLandRoutes.put(key, data.getMap().getRoute(ter1, ter2, DMatches.TerritoryIsLandAndPassable));
    }
    return CachedPassableLandRoutes.get(key);
  }

  /**
   * The same as data.getMap().getWaterRoute(ter1, ter2), except that this method caches the resulting Route for quick
   * retrieval later on.
   */
  public static Route GetSeaRoute(final GameData data, final Territory ter1, final Territory ter2) {
    final List key = DUtils.ToList(DUtils.ToArray(ter1, ter2));
    if (!CachedSeaRoutes.containsKey(key)) {
      CachedSeaRoutes.put(key, data.getMap().getWaterRoute(ter1, ter2));
    }
    return CachedSeaRoutes.get(key);
  }
}
