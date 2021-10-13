package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.Matches;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.triplea.util.Tuple;

/**
 * Utility class with methods to pick players for the battle calculator.
 *
 * <p>Throughout the class the term "neutral player" is used, this denotes a player who is neither
 * at war nor allied with another player.
 */
public class OpponentSelector {

  /** Utility class, no instances please. */
  private OpponentSelector() {}

  /**
   * Set initial attacker and defender.
   *
   * <p>Please read the source code for the order of the players and conditions involved.
   */
  public static Tuple<Optional<GamePlayer>, Optional<GamePlayer>> getAttackerAndDefender(
      final Territory territory, final GameData data) {
    if (territory == null) {
      // Not much to derive here. Pick attacker first, then defender and priorities the current
      // player if possible.
      return getAttackerAndDefenderWithCurrentPlayerPriority(data);
    } else {
      data.acquireReadLock();
      try {
        // When deciding for a player, usually a player with more units is more important and more
        // likely to be meant, e.g. a territory with 10 units of player A and 1 unit of player B.
        // Thus, we use lists and ordered streams.
        final List<GamePlayer> playersWithUnits =
            territory.getUnitCollection().getPlayersByUnitCount();
        // Within the list, we want to prioritise the current player, so put them on the first spot.
        getCurrentPlayer(data)
            .ifPresent(
                cp -> {
                  if (playersWithUnits.contains(cp)) {
                    playersWithUnits.remove(cp);
                    playersWithUnits.add(0, cp);
                  }
                });
        final List<GamePlayer> playersAtWar =
            playersWithUnits.stream()
                .filter(
                    Matches.isAtWarWithAnyOfThesePlayers(
                        playersWithUnits, data.getRelationshipTracker()))
                .collect(Collectors.toList());

        final boolean isWarOnTerritory = !playersAtWar.isEmpty();
        if (isWarOnTerritory) {
          // Choose as attacker the player who is at war and has the most units and a suitable
          // defender (both times prioritising the current player).
          return getAttackerAndDefenderWithPriorityList(playersAtWar, data);
        } else {
          // No fighting is going on. Assume an attack on this territory and pick the defender
          // first, then the attacker.
          final boolean areUnitsOnTerritory = !playersWithUnits.isEmpty();
          if (areUnitsOnTerritory) {
            assert (!playersWithUnits.isEmpty());
            final GamePlayer defender = playersWithUnits.get(0);
            final Optional<GamePlayer> attacker =
                getOpponentWithCurrentPlayerPriority(defender, data);
            return Tuple.of(attacker, Optional.of(defender));
          } else {
            // Empty territory.
            final GamePlayer territoryOwner = territory.getOwner();
            if (!territoryOwner.isNull()) {
              // If the territory has an owner, use it as defender and pick the attacker as enemy if
              // possible.
              final GamePlayer defender = territoryOwner;
              final Optional<GamePlayer> attacker =
                  getOpponentWithCurrentPlayerPriority(territoryOwner, data);
              return Tuple.of(attacker, Optional.of(defender));
            } else {
              // Empty territory without owner. Pick attacker first, then defender and priorities
              // the current player if possible.
              return getAttackerAndDefenderWithCurrentPlayerPriority(data);
            }
          }
        }
      } finally {
        data.releaseReadLock();
      }
    }
  }

  /**
   * First pick an attacker and then a suitable defender while prioritising the current player.
   *
   * <p>The attacker is picked with following priorities
   *
   * <ol>
   *   <li>the current player
   *   <li>any player
   * </ol>
   *
   * <p>The defender is chosen with the following priorities
   *
   * <ol>
   *   <li>any enemy of the attacker
   *   <li>any neutral player (with respect to the attacker)
   *   <li>any player
   * </ol>
   *
   * <p>If the game has no players, empty optionals are returned.
   *
   * @param data the game data
   * @return tuple of attacker (first) and defender (second)
   */
  private static Tuple<Optional<GamePlayer>, Optional<GamePlayer>>
      getAttackerAndDefenderWithCurrentPlayerPriority(final GameData data) {
    return getAttackerAndDefenderWithPriorityList(
        getCurrentPlayer(data).stream().collect(Collectors.toList()), data);
  }

  /**
   * First pick an attacker and then a suitable defender while prioritising players in {@code
   * priorityPlayers}. The order in {@code priorityPlayers} determines the priority for those
   * players included in that list. Players not in the list are at the bottom without any order.
   *
   * <p>The attacker is picked with following priorities
   *
   * <ol>
   *   <li>the players in {@code priorityPlayers} (in that order)
   *   <li>any player
   * </ol>
   *
   * <p>The defender is chosen with the following priorities
   *
   * <ol>
   *   <li>the first player in {@code priorityPlayers} who is an enemy of the attacker
   *   <li>any enemy of the attacker
   *   <li>the first player {@code priorityPlayers} who is neutral towards the attacker
   *   <li>any neutral player (with respect to the attacker)
   *   <li>any player
   * </ol>
   *
   * <p>If the game has no players, empty optionals are returned.
   *
   * @param priorityPlayers an ordered list of players which should be considered first
   * @param data the game data
   * @return tuple of attacker (first) and defender (second)
   */
  private static Tuple<Optional<GamePlayer>, Optional<GamePlayer>>
      getAttackerAndDefenderWithPriorityList(
          final List<GamePlayer> priorityPlayers, final GameData data) {
    // Attacker
    final Optional<GamePlayer> attacker =
        Stream.of(priorityPlayers.stream(), data.getPlayerList().stream())
            .flatMap(s -> s)
            .findFirst();
    if (attacker.isEmpty()) {
      return Tuple.of(Optional.empty(), Optional.empty());
    }
    // Defender
    assert (!attacker.isEmpty());
    final Optional<GamePlayer> defender =
        getOpponentWithPriorityList(attacker.get(), priorityPlayers, data);
    return Tuple.of(attacker, defender);
  }

  /**
   * Return a suitable opponent for player {@code p} with players in {@code priorityPlayers} given
   * priority. The order in {@code priorityPlayers} determines the priority for those players
   * included in that list. Players not in the list are at the bottom without any order.
   *
   * <p>The opponent is chosen with the following priorities
   *
   * <ol>
   *   <li>the first player in {@code priorityPlayers} who is an enemy of {@code p}
   *   <li>any enemy of {@code p}
   *   <li>the first player {@code priorityPlayers} who is neutral towards {@code p}
   *   <li>any neutral player (with respect to {@code p})
   *   <li>any player
   * </ol>
   *
   * @param p the player to find an opponent for
   * @param priorityPlayers an ordered list of players which should be considered first
   * @param data the game data
   * @return an opponent. An empty optional is returned if the game has no players
   */
  private static Optional<GamePlayer> getOpponentWithPriorityList(
      final GamePlayer p, final List<GamePlayer> priorityPlayers, final GameData data) {
    final Stream<GamePlayer> enemiesPriority =
        priorityPlayers.stream().filter(Matches.isAtWar(p, data.getRelationshipTracker()));
    final Stream<GamePlayer> neutralsPriority =
        priorityPlayers.stream()
            .filter(Matches.isAtWar(p, data.getRelationshipTracker()).negate())
            .filter(Matches.isAtWar(p, data.getRelationshipTracker()).negate());
    return Stream.of(
            enemiesPriority,
            playersAtWarWith(p, data),
            neutralsPriority,
            neutralPlayersTowards(p, data),
            data.getPlayerList().stream())
        .flatMap(s -> s)
        .findFirst();
  }

  /**
   * Pick a suitable opponent for player {@code p} while prioritising the current player.
   *
   * <p>The opponent is chosen with the following priorities
   *
   * <ol>
   *   <li>the current player if they is an enemy of {@code p}
   *   <li>any enemy of {@code p}
   *   <li>the current player if they is neutral towards {@code p}
   *   <li>any neutral player (with respect to {@code p})
   *   <li>any player
   * </ol>
   *
   * @param p the player to find an opponent for
   * @param data the game data
   * @return an opponent. An empty optional is returned if the game has no players
   */
  private static Optional<GamePlayer> getOpponentWithCurrentPlayerPriority(
      final GamePlayer p, final GameData data) {
    return getOpponentWithPriorityList(
        p, getCurrentPlayer(data).stream().collect(Collectors.toList()), data);
  }

  private static Optional<GamePlayer> getCurrentPlayer(final GameData data) {
    final Optional<GamePlayer> player = data.getHistory().getActivePlayer();
    if (player.isPresent()) {
      return player;
    }
    return GamePlayer.asOptional(data.getSequence().getStep().getPlayerId());
  }

  /**
   * Returns a stream of all players which are at war with player {@code p}.
   *
   * <p>The returned stream might be empty.
   */
  private static Stream<GamePlayer> playersAtWarWith(final GamePlayer p, final GameData data) {
    return data.getPlayerList().stream().filter(Matches.isAtWar(p, data.getRelationshipTracker()));
  }

  /**
   * Returns a stream of all players which are neither allied nor at war with player {@code p}.
   *
   * <p>The returned stream might be empty.
   */
  private static Stream<GamePlayer> neutralPlayersTowards(final GamePlayer p, final GameData data) {
    return data.getPlayerList().stream()
        .filter(Matches.isAtWar(p, data.getRelationshipTracker()).negate())
        .filter(Matches.isAllied(p, data.getRelationshipTracker()).negate());
  }
}
