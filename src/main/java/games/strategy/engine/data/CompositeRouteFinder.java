package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

public class CompositeRouteFinder {
  private static final Logger s_logger = Logger.getLogger(CompositeRouteFinder.class.getName());

  private final GameMap m_map;
  private final HashMap<Match<Territory>, Integer> m_matches;

  /**
   * This class can find composite routes between two territories.
   * Example set of matches: [Friendly Land, score: 1] [Enemy Land, score: 2] [Neutral Land, score = 4]
   * With this example set, an 8 length friendly route is considered equal in score to a 4 length enemy route and a 2
   * length neutral route.
   * This is because the friendly route score is 1/2 of the enemy route score and 1/4 of the neutral route score.
   * Note that you can choose whatever scores you want, and that the matches can mix and match with each other in any
   * way.
   *
   * @param map
   *        - Game map found through &lt;gamedata>.getMap()
   * @param matches
   *        - Set of matches and scores. The lower a match is scored, the more favorable it is.
   */
  public CompositeRouteFinder(final GameMap map, final HashMap<Match<Territory>, Integer> matches) {
    m_map = map;
    m_matches = matches;
    s_logger.finer("Initializing CompositeRouteFinderClass...");
  }

  private HashSet<Territory> ToHashSet(final Collection<Territory> ters) {
    final HashSet<Territory> result = new HashSet<>();
    for (final Territory ter : ters) {
      result.add(ter);
    }
    return result;
  }

  public Route findRoute(final Territory start, final Territory end) {
    final HashSet<Territory> allMatchingTers =
        ToHashSet(Match.getMatches(m_map.getTerritories(), new CompositeMatchOr<>(m_matches.keySet())));
    final HashMap<Territory, Integer> terScoreMap = CreateScoreMap();
    final HashMap<Territory, Integer> routeScoreMap = new HashMap<>();
    int bestRouteToEndScore = Integer.MAX_VALUE;
    final HashMap<Territory, Territory> previous = new HashMap<>();
    List<Territory> routeLeadersToProcess = new ArrayList<>();
    for (final Territory ter : m_map.getNeighbors(start, Matches.territoryIsInList(allMatchingTers))) {
      final int routeScore = terScoreMap.get(start) + terScoreMap.get(ter);
      routeScoreMap.put(ter, routeScore);
      routeLeadersToProcess.add(ter);
      previous.put(ter, start);
    }
    while (routeLeadersToProcess.size() > 0) {
      final List<Territory> newLeaders = new ArrayList<>();
      for (final Territory oldLeader : routeLeadersToProcess) {
        for (final Territory ter : m_map.getNeighbors(oldLeader, Matches.territoryIsInList(allMatchingTers))) {
          final int routeScore = routeScoreMap.get(oldLeader) + terScoreMap.get(ter);
          if (routeLeadersToProcess.contains(ter) || ter.equals(start)) {
            continue;
          }
          if (previous.containsKey(ter)) { // If we're bumping into an existing route
            if (routeScore >= routeScoreMap.get(ter)) {
              continue;
            }
          }
          if (bestRouteToEndScore <= routeScore) {
            // Ignore this route leader, as we know we already have a better route
            continue;
          }
          routeScoreMap.put(ter, routeScore);
          newLeaders.add(ter);
          previous.put(ter, oldLeader);
          if (ter.equals(end)) {
            if (routeScore < bestRouteToEndScore) {
              bestRouteToEndScore = routeScore;
            }
          }
        }
      }
      routeLeadersToProcess = newLeaders;
    }
    if (bestRouteToEndScore == Integer.MAX_VALUE) {
      return null;
    }
    return AssembleRoute(start, end, previous);
  }

  private Route AssembleRoute(final Territory start, final Territory end,
      final HashMap<Territory, Territory> previous) {
    final List<Territory> routeTers = new ArrayList<>();
    Territory curTer = end;
    while (previous.containsKey(curTer)) {
      routeTers.add(curTer);
      curTer = previous.get(curTer);
    }
    routeTers.add(start);
    Collections.reverse(routeTers);
    return new Route(routeTers);
  }

  private HashMap<Territory, Integer> CreateScoreMap() {
    final HashMap<Territory, Integer> result = new HashMap<>();
    for (final Territory ter : m_map.getTerritories()) {
      result.put(ter, getTerScore(ter));
    }
    return result;
  }

  /*
   * Returns the score of the best match that matches this territory
   */
  private int getTerScore(final Territory ter) {
    int bestMatchingScore = Integer.MAX_VALUE;
    for (final Match<Territory> match : m_matches.keySet()) {
      final int score = m_matches.get(match);
      if (score < bestMatchingScore) { // If this is a 'better' match
        if (match.match(ter)) {
          bestMatchingScore = score;
        }
      }
    }
    return bestMatchingScore;
  }
}
