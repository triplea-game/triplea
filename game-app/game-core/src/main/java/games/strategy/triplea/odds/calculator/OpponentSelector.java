package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;

/**
 * Utility class with methods to pick players for the battle calculator.
 *
 * <p>Throughout the class the term "neutral player" is used, this denotes a player who is neither
 * at war nor allied with another player.
 */
@Builder(access = AccessLevel.PACKAGE)
public class OpponentSelector {

  /** Value object class for an attacker and defender tuple. */
  @Builder
  @Value
  public static class AttackerAndDefender {
    @Builder.Default private final Optional<GamePlayer> attacker = Optional.empty();
    @Builder.Default private final Optional<GamePlayer> defender = Optional.empty();
  }

  // Store all data needed for the selection process individually to be independent of the
  // GameData object.
  @Builder.Default private final Collection<GamePlayer> players = new ArrayList(0);
  @Nullable private final GamePlayer currentPlayer;
  @Nonnull private final RelationshipTracker relationshipTracker;

  public static OpponentSelector with(final GameData gameData) {
    try {
      gameData.acquireReadLock();
      return OpponentSelector.builder()
          .players(gameData.getPlayerList().getPlayers())
          .currentPlayer(gameData.getSequence().getStep().getPlayerId())
          .relationshipTracker(gameData.getRelationshipTracker())
          .build();
    } finally {
      gameData.releaseReadLock();
    }
  }

  /**
   * Set initial attacker and defender.
   *
   * <p>Please read the source code for the order of the players and conditions involved.
   */
  public AttackerAndDefender getAttackerAndDefender(final Territory territory) {
    if (territory == null) {
      // Not much to derive here. Pick attacker first, then defender and priorities the current
      // player if possible.
      return getAttackerAndDefenderWithCurrentPlayerPriority();
    } else {
      // If there is no current player, we cannot choose an opponent.
      if (currentPlayer == null) {
        return AttackerAndDefender.builder().build();
      }

      // Select the defender to be an enemy of the current player if possible, preferring enemies
      // in
      // the given territory.
      // When deciding for an enemy, usually a player with more units is more important and more
      // likely to be meant, e.g. a territory with 10 units of player A and 1 unit of player B.
      // Thus, we use lists and ordered streams.
      final List<GamePlayer> playersWithUnits =
          territory.getUnitCollection().getPlayersByUnitCount();
      final Optional<GamePlayer> defender =
          getOpponentWithPriorityList(currentPlayer, playersWithUnits);

      return AttackerAndDefender.builder()
          .attacker(Optional.ofNullable(currentPlayer))
          .defender(defender)
          .build();
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
   * @return attacker and defender
   */
  private AttackerAndDefender getAttackerAndDefenderWithCurrentPlayerPriority() {
    return getAttackerAndDefenderWithPriorityList(List.of(currentPlayer));
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
   * @return attacker and defender
   */
  private AttackerAndDefender getAttackerAndDefenderWithPriorityList(
      final List<GamePlayer> priorityPlayers) {
    // Attacker
    final Optional<GamePlayer> attacker =
        Stream.of(priorityPlayers.stream(), players.stream()).flatMap(s -> s).findFirst();
    if (attacker.isEmpty()) {
      return AttackerAndDefender.builder()
          .attacker(Optional.empty())
          .defender(Optional.empty())
          .build();
    }
    // Defender
    assert (!attacker.isEmpty());
    final Optional<GamePlayer> defender =
        getOpponentWithPriorityList(attacker.get(), priorityPlayers);
    return AttackerAndDefender.builder().attacker(attacker).defender(defender).build();
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
   * @return an opponent. An empty optional is returned if the game has no players
   */
  private Optional<GamePlayer> getOpponentWithPriorityList(
      final GamePlayer p, final List<GamePlayer> priorityPlayers) {
    final Stream<GamePlayer> enemiesPriority =
        priorityPlayers.stream().filter(Matches.isAtWar(p, relationshipTracker));
    final Stream<GamePlayer> neutralsPriority =
        priorityPlayers.stream()
            .filter(Matches.isAtWar(p, relationshipTracker).negate())
            .filter(Matches.isAllied(p, relationshipTracker).negate());
    return Stream.of(
            enemiesPriority,
            playersAtWarWith(p),
            neutralsPriority,
            neutralPlayersTowards(p),
            players.stream())
        .flatMap(s -> s)
        .findFirst();
  }

  /**
   * Returns a stream of all players which are at war with player {@code p}.
   *
   * <p>The returned stream might be empty.
   */
  private Stream<GamePlayer> playersAtWarWith(final GamePlayer p) {
    return players.stream().filter(Matches.isAtWar(p, relationshipTracker));
  }

  /**
   * Returns a stream of all players which are neither allied nor at war with player {@code p}.
   *
   * <p>The returned stream might be empty.
   */
  private Stream<GamePlayer> neutralPlayersTowards(final GamePlayer p) {
    return players.stream()
        .filter(Matches.isAtWar(p, relationshipTracker).negate())
        .filter(Matches.isAllied(p, relationshipTracker).negate());
  }
}
