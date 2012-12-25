package games.strategy.triplea.ai.weakAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.AIUtils;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("deprecation")
public class Utils
{
	/**
	 * All the territories that border one of our territories
	 */
	public static List<Territory> getNeighboringEnemyLandTerritories(final GameData data, final PlayerID player)
	{
		final ArrayList<Territory> rVal = new ArrayList<Territory>();
		for (final Territory t : data.getMap())
		{
			if (Matches.isTerritoryEnemy(player, data).match(t) && !t.getOwner().isNull())
			{
				if (!data.getMap().getNeighbors(t, Matches.isTerritoryOwnedBy(player)).isEmpty())
				{
					rVal.add(t);
				}
			}
		}
		return rVal;
	}
	
	public static List<Unit> getUnitsUpToStrength(final double maxStrength, final Collection<Unit> units, final boolean attacking, final boolean sea)
	{
		if (AIUtils.strength(units, attacking, sea) < maxStrength)
			return new ArrayList<Unit>(units);
		final ArrayList<Unit> rVal = new ArrayList<Unit>();
		for (final Unit u : units)
		{
			rVal.add(u);
			if (AIUtils.strength(rVal, attacking, sea) > maxStrength)
				return rVal;
		}
		return rVal;
	}
	
	public static float getStrengthOfPotentialAttackers(final Territory location, final GameData data)
	{
		float strength = 0;
		for (final Territory t : data.getMap().getNeighbors(location, location.isWater() ? Matches.TerritoryIsWater : Matches.TerritoryIsLand))
		{
			final List<Unit> enemies = t.getUnits().getMatches(Matches.enemyUnit(location.getOwner(), data));
			strength += AIUtils.strength(enemies, true, location.isWater());
		}
		return strength;
	}
	
	public static Route findNearest(final Territory start, final Match<Territory> endCondition, final Match<Territory> routeCondition, final GameData data)
	{
		Route shortestRoute = null;
		for (final Territory t : data.getMap().getTerritories())
		{
			if (endCondition.match(t))
			{
				final CompositeMatchOr<Territory> routeOrEnd = new CompositeMatchOr<Territory>(routeCondition, Matches.territoryIs(t));
				final Route r = data.getMap().getRoute(start, t, routeOrEnd);
				if (r != null)
				{
					if (shortestRoute == null || r.getLength() < shortestRoute.getLength())
						shortestRoute = r;
				}
			}
		}
		return shortestRoute;
	}
	
	public static boolean hasLandRouteToEnemyOwnedCapitol(final Territory t, final PlayerID us, final GameData data)
	{
		for (final PlayerID player : Match.getMatches(data.getPlayerList().getPlayers(), Matches.isAtWar(us, data)))
		{
			for (final Territory capital : TerritoryAttachment.getAllCurrentlyOwnedCapitals(player, data))
			{
				if (data.getMap().getDistance(t, capital, Matches.TerritoryIsLand) != -1)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	// returns all territories that are water territories (veqryn)
	public static List<Territory> onlyWaterTerr(final GameData data, final List<Territory> allTerr)
	{
		final List<Territory> water = new ArrayList<Territory>(allTerr);
		final Iterator<Territory> wFIter = water.iterator();
		while (wFIter.hasNext())
		{
			final Territory waterFact = wFIter.next();
			if (!Matches.TerritoryIsWater.match(waterFact))
				wFIter.remove();
		}
		return water;
	}
	
	/**
	 * Return Territories containing any unit depending on unitCondition
	 * Differs from findCertainShips because it doesn't require the units be owned
	 */
	public static List<Territory> findUnitTerr(final GameData data, final PlayerID player, final Match<Unit> unitCondition)
	{
		// Return territories containing a certain unit or set of Units
		final CompositeMatch<Unit> limitShips = new CompositeMatchAnd<Unit>(unitCondition);
		final List<Territory> shipTerr = new ArrayList<Territory>();
		final Collection<Territory> tNeighbors = data.getMap().getTerritories();
		for (final Territory t2 : tNeighbors)
		{
			if (t2.getUnits().someMatch(limitShips))
				shipTerr.add(t2);
		}
		return shipTerr;
	}
}
