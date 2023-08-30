package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.UnitUtils;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Utility class with methods to pick players for the battle calculator.
 *
 * <p>Throughout the class the term "neutral player" is used, this denotes a player who is neither
 * at war nor allied with another player.
 */
@Builder(access = AccessLevel.PACKAGE)
@RequiredArgsConstructor
public class AttackerAndDefenderSelector {

  /** Value object class for an attacker and defender tuple. */
  @Builder
  @Value
  public static class AttackerAndDefender {
    /** NONE = No attacker, no defender and no units. */
    public static final AttackerAndDefender NONE = AttackerAndDefender.builder().build();

    @Nullable GamePlayer attacker;
    @Nullable GamePlayer defender;
    @Builder.Default List<Unit> attackingUnits = List.of();
    @Builder.Default List<Unit> defendingUnits = List.of();

    public Optional<GamePlayer> getAttacker() {
      return Optional.ofNullable(attacker);
    }

    public Optional<GamePlayer> getDefender() {
      return Optional.ofNullable(defender);
    }
  }

  @Nonnull private final Collection<GamePlayer> players;
  @Nullable private final GamePlayer currentPlayer;
  @Nonnull private final RelationshipTracker relationshipTracker;
  @Nullable private final Territory territory;

  /**
   * Set initial attacker and defender.
   *
   * <p>Please read the source code for the order of the players and conditions involved.
   */
  public AttackerAndDefender getAttackerAndDefender() {
    // If there is no current player, we cannot choose an opponent.
    if (currentPlayer == null) {
      return AttackerAndDefender.NONE;
    }

    if (territory == null) {
      // Without territory, we cannot prioritize any players (except current player); no units to
      // select.
      return getAttackerAndDefenderWithPriorityList(List.of(currentPlayer));
    } else {
      // Select the defender to be an enemy of the current player if possible, preferring enemies
      // in the given territory. When deciding for an enemy, usually a player with more units is
      // more important and more likely to be meant, e.g. a territory with 10 units of player A and
      // 1 unit of player B. Thus, we use lists and ordered streams.
      final List<GamePlayer> playersWithUnits =
          territory.getUnitCollection().getPlayersByUnitCount();
      // Add the territory owner add the end of the priority list.  This way, when attacking an
      // empty territory, the owner gets preferred even though they have no units in their land. In
      // case the owner has units in the land, then they are already in the list but adding a second
      // entry to the list doesn't impact the algorithm.
      final GamePlayer territoryOwner = territory.getOwner();
      if (!territoryOwner.isNull()) {
        playersWithUnits.add(territoryOwner);
      }

      final GamePlayer attacker = currentPlayer;
      // Attacker fights alone; the defender can also use all the allied units.
      final GamePlayer defender =
          getOpponentWithPriorityList(territory, attacker, playersWithUnits).orElse(null);
      final List<Unit> attackingUnits =
          territory.getUnitCollection().getMatches(Matches.unitIsOwnedBy(attacker));
      final List<Unit> defendingUnits =
          defender == null
              ? List.of()
              : territory.getUnitCollection().getMatches(Matches.alliedUnit(defender));

      return AttackerAndDefender.builder()
          .attacker(attacker)
          .defender(defender)
          .attackingUnits(attackingUnits)
          .defendingUnits(defendingUnits)
          .build();
    }
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
    final GamePlayer attacker =
        Stream.of(priorityPlayers.stream(), players.stream())
            .flatMap(s -> s)
            .findFirst()
            .orElse(null);
    if (attacker == null) {
      return AttackerAndDefender.NONE;
    }
    // Defender
    final GamePlayer defender =
        getOpponentWithPriorityList(territory, attacker, priorityPlayers).orElse(null);
    return AttackerAndDefender.builder().attacker(attacker).defender(defender).build();
  }

  /**
   * Return a suitable opponent for player {@code p} with players in {@code priorityPlayers} given
   * priority. The order in {@code priorityPlayers} determines the priority for those players
   * included in that list. Players not in the list are at the bottom without any order.
   *
   * <p>Some additional prioritisation is given based on the territory owner and players with units.
   * Otherwise, the opponent is chosen with the following priorities
   *
   * <ol>
   *   <li>the first player in {@code priorityPlayers} who is an enemy of {@code p}
   *   <li>any enemy of {@code p}
   *   <li>the first player {@code priorityPlayers} who is neutral towards {@code p}
   *   <li>any neutral player (with respect to {@code p})
   *   <li>any player
   * </ol>
   *
   * @param player the player to find an opponent for
   * @param priorityPlayers an ordered list of players which should be considered first
   * @return an opponent. An empty optional is returned if the game has no players
   */
  private Optional<GamePlayer> getOpponentWithPriorityList(
      Territory territory, final GamePlayer player, final List<GamePlayer> priorityPlayers) {
    GamePlayer bestDefender = null;
    // Handle some special cases that the priority ordering logic doesn't handle. See tests.
    if (territory != null) {
      if (territory.isWater()) {
        bestDefender = getEnemyWithMostUnits(territory);
        if (bestDefender == null) {
          bestDefender = UnitUtils.findPlayerWithMostUnits(territory.getUnits());
        }
      } else {
        bestDefender = territory.getOwner();
        // If we're not at war with the owner and there are enemies, fight them.
        if (!bestDefender.isAtWar(currentPlayer)) {
          GamePlayer enemyWithMostUnits = getEnemyWithMostUnits(territory);
          if (enemyWithMostUnits != null) {
            bestDefender = enemyWithMostUnits;
          }
        }
      }
    }
    final Stream<GamePlayer> enemiesPriority =
        priorityPlayers.stream().filter(Matches.isAtWar(player));
    final Stream<GamePlayer> neutralsPriority =
        priorityPlayers.stream()
            .filter(Matches.isAtWar(player).negate())
            .filter(Matches.isAllied(player).negate());
    return Stream.of(
            Optional.ofNullable(bestDefender).stream(),
            enemiesPriority,
            playersAtWarWith(player),
            neutralsPriority,
            neutralPlayersTowards(player),
            players.stream())
        .flatMap(s -> s)
        .findFirst();
  }

  private @Nullable GamePlayer getEnemyWithMostUnits(Territory territory) {
    return UnitUtils.findPlayerWithMostUnits(
        territory.getUnits().stream()
            .filter(Matches.unitIsEnemyOf(currentPlayer))
            .collect(Collectors.toList()));
  }

  /**
   * Returns a stream of all players which are at war with player {@code p}.
   *
   * <p>The returned stream might be empty.
   */
  private Stream<GamePlayer> playersAtWarWith(final GamePlayer p) {
    return players.stream().filter(Matches.isAtWar(p));
  }

  /**
   * Returns a stream of all players which are neither allied nor at war with player {@code p}.
   *
   * <p>The returned stream might be empty.
   */
  private Stream<GamePlayer> neutralPlayersTowards(final GamePlayer p) {
    return players.stream()
        .filter(Matches.isAtWar(p).negate())
        .filter(Matches.isAllied(p).negate());
  }
}
