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

package games.strategy.engine.data;

import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

public class CompositeRouteFinder
{
	private final static Logger s_logger = Logger.getLogger(CompositeRouteFinder.class.getName());
	
	public static Logger GetStaticLogger()
	{
		return s_logger;
	}
	
	private final GameMap m_map;
	private final HashMap<Match<Territory>, Integer> m_matches;
	
	/**
	 * This class can find composite routes between two territories.
	 * 
	 * Example set of matches: [Friendly Land, score: 1] [Enemy Land, score: 2] [Neutral Land, score = 4]
	 * 
	 * With this example set, an 8 length friendly route is considered equal in score to a 4 length enemy route and a 2 length neutral route.
	 * This is because the friendly route score is 1/2 of the enemy route score and 1/4 of the neutral route score.
	 * 
	 * Note that you can choose whatever scores you want, and that the matches can mix and match with each other in any way.
	 * 
	 * @param map
	 *            - Game map found through <gamedata>.getMap()
	 * @param matches
	 *            - Set of matches and scores. The lower a match is scored, the more favorable it is.
	 */
	public CompositeRouteFinder(GameMap map, HashMap<Match<Territory>, Integer> matches)
	{
		m_map = map;
		m_matches = matches;
		s_logger.finer("Initializing CompositeRouteFinderClass...");
	}
	
	private HashSet<Territory> ToHashSet(Collection<Territory> ters)
	{
		HashSet<Territory> result = new HashSet<Territory>();
		for (Territory ter : ters)
			result.add(ter);
		return result;
	}
	
	public Route findRoute(Territory start, Territory end)
	{
		HashSet<Territory> allMatchingTers = ToHashSet(Match.getMatches(m_map.getTerritories(), new CompositeMatchOr<Territory>(m_matches.keySet())));
		
		HashMap<Territory, Integer> terScoreMap = CreateScoreMap(allMatchingTers, start);
		HashMap<Territory, Integer> routeScoreMap = new HashMap<Territory, Integer>();
		int bestRouteToEndScore = Integer.MAX_VALUE;
		
		HashMap<Territory, Territory> previous = new HashMap<Territory, Territory>();
		List<Territory> routeLeadersToProcess = new ArrayList<Territory>();
		for (Territory ter : m_map.getNeighbors(start, Matches.territoryIsInList(allMatchingTers)))
		{
			int routeScore = terScoreMap.get(start) + terScoreMap.get(ter);
			routeScoreMap.put(ter, routeScore);
			routeLeadersToProcess.add(ter);
			previous.put(ter, start);
		}
		while (routeLeadersToProcess.size() > 0)
		{
			List<Territory> newLeaders = new ArrayList<Territory>();
			for (Territory oldLeader : routeLeadersToProcess)
			{
				for (Territory ter : m_map.getNeighbors(oldLeader, Matches.territoryIsInList(allMatchingTers)))
				{
					int routeScore = routeScoreMap.get(oldLeader) + terScoreMap.get(ter);
					if (routeLeadersToProcess.contains(ter) || ter.equals(start)) // If we're bumping into one of the current route leaders or the start
						continue;
					if (previous.containsKey(ter)) // If we're bumping into an existing route
					{
						if (routeScore >= routeScoreMap.get(ter)) // If the already existing route route is better or the same
							continue;
					}
					if (bestRouteToEndScore <= routeScore)
						continue; // Ignore this route leader, as we know we already have a better route
					routeScoreMap.put(ter, routeScore);
					newLeaders.add(ter);
					previous.put(ter, oldLeader);
					if (ter.equals(end))
					{
						if (routeScore < bestRouteToEndScore)
							bestRouteToEndScore = routeScore;
					}
				}
			}
			routeLeadersToProcess = newLeaders;
		}
		if (bestRouteToEndScore == Integer.MAX_VALUE)
			return null;
		return AssembleRoute(start, end, previous);
	}
	
	private Route AssembleRoute(Territory start, Territory end, HashMap<Territory, Territory> previous)
	{
		List<Territory> routeTers = new ArrayList<Territory>();
		Territory curTer = end;
		while (previous.containsKey(curTer))
		{
			routeTers.add(curTer);
			curTer = previous.get(curTer);
		}
		routeTers.add(start);
		Collections.reverse(routeTers);
		return new Route(routeTers);
	}
	
	private HashMap<Territory, Integer> CreateScoreMap(Collection<Territory> ters, Territory startTer)
	{
		HashMap<Territory, Integer> result = new HashMap<Territory, Integer>();
		for (Territory ter : m_map.getTerritories())
		{
			result.put(ter, GetTerScore(ter));
		}
		return result;
	}
	
	/*
	 * Returns the score of the best match that matches this territory
	 */
	private Integer GetTerScore(Territory ter)
	{
		int bestMatchingScore = Integer.MAX_VALUE;
		for (Match<Territory> match : m_matches.keySet())
		{
			int score = m_matches.get(match);
			if (score < bestMatchingScore) // If this is a 'better' match
			{
				if (match.match(ter))
				{
					bestMatchingScore = score;
				}
			}
		}
		return bestMatchingScore;
	}
}
