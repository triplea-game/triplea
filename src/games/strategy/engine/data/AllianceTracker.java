package games.strategy.engine.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Tracks alliances between players.
 * An alliance is a named entity, players are added to an alliance.
 * Currently only used for tracking stats (like TUV, total production, etc), and for tracking total victory cities for
 * alliance based
 * victory conditions.
 * Not used for determining in-game alliances (instead, see the Relationship tracker for that).
 */
public class AllianceTracker extends GameDataComponent {
  private static final long serialVersionUID = 2815023984535209353L;
  // maps PlayerID to Collection of alliances names
  Map<PlayerID, Collection<String>> m_alliances = new HashMap<>();

  public AllianceTracker(final GameData data) {
    super(data);
  }

  /**
   * Adds PlayerID player to the alliance specified by allianceName.
   *
   * @param player
   *        The player to add to the alliance.
   * @param allianceName
   *        The alliance to add to.
   */
  protected void addToAlliance(final PlayerID player, final String allianceName) {
    if (!m_alliances.containsKey(player)) {
      final Collection<String> alliances = new HashSet<>();
      alliances.add(allianceName);
      m_alliances.put(player, alliances);
    } else {
      final Collection<String> alliances = m_alliances.get(player);
      alliances.add(allianceName);
    }
  }

  /**
   * @return a set of all the games alliances, this will return an empty set if you aren't using alliances
   */
  public Set<String> getAlliances() {
    final Iterator<PlayerID> keys = m_alliances.keySet().iterator();
    final Set<String> rVal = new HashSet<>();
    while (keys.hasNext()) {
      rVal.addAll(m_alliances.get(keys.next()));
    }
    return rVal;
  }

  /**
   * Returns the PlayerID's that are members of the alliance
   * specified by the String allianceName
   *
   * @param allianceName
   *        Alliance name
   * @return all the players in the given alliance
   */
  public HashSet<PlayerID> getPlayersInAlliance(final String allianceName) {
    final Iterator<PlayerID> keys = m_alliances.keySet().iterator();
    final HashSet<PlayerID> rVal = new HashSet<>();
    while (keys.hasNext()) {
      final PlayerID player = keys.next();
      final Collection<String> alliances = m_alliances.get(player);
      if (alliances.contains(allianceName)) {
        rVal.add(player);
      }
    }
    return rVal;
  }

  public Collection<String> getAlliancesPlayerIsIn(final PlayerID player) {
    final Collection<String> rVal = m_alliances.get(player);
    if (rVal == null) {
      System.out.println("Player, " + player.getName() + ", is not a member of any alliance!");
      return Collections.singleton(player.getName());
    }
    return rVal;
  }

  public Map<PlayerID, Collection<String>> getAlliancesMap() {
    return m_alliances;
  }
}
