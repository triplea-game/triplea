/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.Dynamix_AI.CommandCenter;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.delegate.Matches;

import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author Stephen
 */
public class CachedCalculationCenter
{
	public static HashMap<Territory, List<Territory>> CachedMapTersFromPoints = new HashMap<Territory, List<Territory>>();
	public static HashMap<List<Territory>, Route> CachedRoutes = new HashMap<List<Territory>, Route>();
	public static HashMap<List<Territory>, Route> CachedAirPassableRoutes = new HashMap<List<Territory>, Route>();
	public static HashMap<List<Territory>, Route> CachedLandRoutes = new HashMap<List<Territory>, Route>();
	public static HashMap<List<Territory>, Route> CachedPassableLandRoutes = new HashMap<List<Territory>, Route>();
	public static HashMap<List<Territory>, Route> CachedSeaRoutes = new HashMap<List<Territory>, Route>();
	
	/**
	 * The same as data.getMap().getRoute(ter1, ter2), except that this method caches the resulting List<Territory> for quick retrieval later on.
	 */
	public static List<Territory> GetMapTersFromPoint(Territory target)
	{
		Territory key = target;
		if (!CachedMapTersFromPoints.containsKey(key))
			CachedMapTersFromPoints.put(key, DUtils.GetTerritoriesWithinXDistanceOfY(target.getData(), target, Integer.MAX_VALUE));
		
		return CachedMapTersFromPoints.get(key);
	}
	
	/**
	 * The same as data.getMap().getRoute(ter1, ter2), except that this method caches the resulting Route for quick retrieval later on.
	 */
	public static Route GetRoute(GameData data, Territory ter1, Territory ter2)
	{
		List key = DUtils.ToList(DUtils.ToArray(ter1, ter2));
		if (!CachedRoutes.containsKey(key))
			CachedRoutes.put(key, data.getMap().getRoute(ter1, ter2));
		
		return CachedRoutes.get(key);
	}
	
	/**
	 * The same as data.getMap().getRoute(ter1, ter2, Matches.TerritoryIsNotImpassable), except that this method caches the resulting Route for quick retrieval later on.
	 */
	public static Route GetAirPassableRoute(GameData data, Territory ter1, Territory ter2)
	{
		List key = DUtils.ToList(DUtils.ToArray(ter1, ter2));
		if (!CachedAirPassableRoutes.containsKey(key))
			CachedAirPassableRoutes.put(key, data.getMap().getRoute(ter1, ter2, Matches.TerritoryIsNotImpassable));
		
		return CachedAirPassableRoutes.get(key);
	}
	
	/**
	 * The same as data.getMap().getLandRoute(ter1, ter2), except that this method caches the resulting Route for quick retrieval later on.
	 */
	public static Route GetLandRoute(GameData data, Territory ter1, Territory ter2)
	{
		List key = DUtils.ToList(DUtils.ToArray(ter1, ter2));
		if (!CachedLandRoutes.containsKey(key))
			CachedLandRoutes.put(key, data.getMap().getLandRoute(ter1, ter2));
		
		return CachedLandRoutes.get(key);
	}
	
	/**
	 * The same as data.getMap().getRoute(ter1, ter2, new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable)), except that this method caches the resulting Route for quick retrieval later on.
	 */
	public static Route GetPassableLandRoute(GameData data, Territory ter1, Territory ter2)
	{
		List key = DUtils.ToList(DUtils.ToArray(ter1, ter2));
		if (!CachedPassableLandRoutes.containsKey(key))
			CachedPassableLandRoutes.put(key, data.getMap().getRoute(ter1, ter2, DMatches.TerritoryIsLandAndPassable));
		
		return CachedPassableLandRoutes.get(key);
	}
	
	/**
	 * The same as data.getMap().getWaterRoute(ter1, ter2), except that this method caches the resulting Route for quick retrieval later on.
	 */
	public static Route GetSeaRoute(GameData data, Territory ter1, Territory ter2)
	{
		List key = DUtils.ToList(DUtils.ToArray(ter1, ter2));
		if (!CachedSeaRoutes.containsKey(key))
			CachedSeaRoutes.put(key, data.getMap().getWaterRoute(ter1, ter2));
		
		return CachedSeaRoutes.get(key);
	}
}
