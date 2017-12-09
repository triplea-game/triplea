package games.strategy.engine.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * Tracks alliances between players.
 * An alliance is a named entity, players are added to an alliance.
 * Currently only used for tracking stats (like TUV, total production, etc), and for tracking total victory cities for
 * alliance based
 * victory conditions.
 * Not used for determining in-game alliances (instead, see the Relationship tracker for that).
 */
public class AllianceTracker implements Serializable {
  private static final long serialVersionUID = 2815023984535209353L;
  // maps PlayerID to Collection of alliances names
  private final Multimap<PlayerID, String> alliances;

  public AllianceTracker() {
    alliances = HashMultimap.create();
  }

  public AllianceTracker(final Multimap<PlayerID, String> alliances) {
    this.alliances = alliances;
  }

  @SerializationProxySupport
  public Object writeReplace() {
    return new SerializationProxy(this);
  }

  private static class SerializationProxy implements Serializable {
    private static final long serialVersionUID = -4193924040595347947L;
    private final Multimap<PlayerID, String> alliances;

    public SerializationProxy(final AllianceTracker allianceTracker) {
      alliances = ImmutableMultimap.copyOf(allianceTracker.alliances);
    }

    private Object readResolve() {
      return new AllianceTracker(alliances);
    }
  }


  /**
   * Adds PlayerID player to the alliance specified by allianceName.
   *
   * @param player The player to add to the alliance.
   * @param allianceName The alliance to add to.
   */
  protected void addToAlliance(final PlayerID player, final String allianceName) {
    alliances.put(player, allianceName);
  }

  /**
   * @return a set of all the games alliances, this will return an empty set if you aren't using alliances.
   */
  public Set<String> getAlliances() {
    return alliances.keySet().stream()
        .map(alliances::get)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  /**
   * Returns the PlayerID's that are members of the alliance
   * specified by the String allianceName.
   *
   * @param allianceName Alliance name
   * @return all the players in the given alliance
   */
  public Set<PlayerID> getPlayersInAlliance(final String allianceName) {
    return alliances.keySet().stream()
        .filter(player -> alliances.get(player).contains(allianceName))
        .collect(Collectors.toSet());
  }

  public Collection<String> getAlliancesPlayerIsIn(final PlayerID player) {
    final Collection<String> alliancesPlayerIsIn = alliances.get(player);
    if (!alliancesPlayerIsIn.isEmpty()) {
      return alliancesPlayerIsIn;
    } else {
      return Collections.singleton(player.getName());
    }
  }

  Set<PlayerID> getAllies(final PlayerID currentPlayer) {
    final Set<PlayerID> allies = new HashSet<>();
    // for each of the player alliances, add each player from that alliance to the total alliance list
    alliances.get(currentPlayer).forEach(alliance -> allies.addAll(getPlayersInAlliance(alliance)));
    return allies;
  }
}
