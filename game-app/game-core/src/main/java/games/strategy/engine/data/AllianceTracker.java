package games.strategy.engine.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tracks alliances between players. An alliance is a named entity, players are added to an
 * alliance. Currently only used for tracking stats (like TUV, total production, etc), and for
 * tracking total victory cities for alliance based victory conditions. Not used for determining
 * in-game alliances (instead, see the Relationship tracker for that).
 */
public class AllianceTracker implements Serializable {
  private static final long serialVersionUID = 2815023984535209353L;
  // maps PlayerId to Collection of alliances names
  private final Multimap<GamePlayer, String> alliances;

  public AllianceTracker() {
    this(HashMultimap.create());
  }

  public AllianceTracker(final Multimap<GamePlayer, String> alliances) {
    this.alliances = alliances;
  }

  @SerializationProxySupport
  public Object writeReplace() {
    return new SerializationProxy(this);
  }

  private static class SerializationProxy implements Serializable {
    private static final long serialVersionUID = -4193924040595347947L;
    private final Multimap<GamePlayer, String> alliances;

    SerializationProxy(final AllianceTracker allianceTracker) {
      alliances = ImmutableMultimap.copyOf(allianceTracker.alliances);
    }

    private Object readResolve() {
      return new AllianceTracker(alliances);
    }
  }

  /**
   * Adds PlayerId player to the alliance specified by allianceName.
   *
   * @param player The player to add to the alliance.
   * @param allianceName The alliance to add to.
   */
  public void addToAlliance(final GamePlayer player, final String allianceName) {
    alliances.put(player, allianceName);
  }

  /**
   * Returns a set of all the games alliances, this will return an empty set if you aren't using
   * alliances.
   */
  public Set<String> getAlliances() {
    return new HashSet<>(alliances.values());
  }

  /**
   * Returns the PlayerId's that are members of the alliance specified by the String allianceName.
   *
   * @param allianceName Alliance name
   * @return all the players in the given alliance
   */
  public Set<GamePlayer> getPlayersInAlliance(final String allianceName) {
    return alliances.entries().stream()
        .filter(e -> e.getValue().equals(allianceName))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  public Collection<String> getAlliancesPlayerIsIn(final GamePlayer player) {
    final Collection<String> alliancesPlayerIsIn = alliances.get(player);
    return !alliancesPlayerIsIn.isEmpty() ? alliancesPlayerIsIn : Set.of(player.getName());
  }

  public Set<GamePlayer> getAllies(final GamePlayer currentPlayer) {
    return alliances.get(currentPlayer).stream()
        .map(this::getPlayersInAlliance)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }
}
