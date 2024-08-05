package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.util.ProUtils;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

  /** Set initial attacker as current player and defender according to priority. */
  public AttackerAndDefender getAttackerAndDefender() {

    final GamePlayer attacker = getBestAttacker();
    if (attacker == null) {
      return AttackerAndDefender.NONE;
    }

    // determine potential defenders (sub set of all players)
    final GamePlayer defender = getBestDefender(attacker);
    if (defender == null) {
      return AttackerAndDefender.NONE;
    }
    if (territory == null) {
      return AttackerAndDefender.builder().attacker(attacker).defender(defender).build();
    }
    final List<Unit> attackingUnits = territory.getMatches(Matches.unitIsOwnedBy(attacker));
    final List<Unit> defendingUnits = territory.getMatches(Matches.alliedUnit(defender));
    return AttackerAndDefender.builder()
        .attacker(attacker)
        .defender(defender)
        .attackingUnits(attackingUnits)
        .defendingUnits(defendingUnits)
        .build();
  }

  /**
   * Returns the "best" attacker, i.e. the current player or next enemy when there are only allied
   * units.
   *
   * @return best attacker
   */
  @Nullable
  private GamePlayer getBestAttacker() {
    if (currentPlayer == null) {
      return null;
    }
    if (territory != null) {
      final Collection<Unit> units = territory.getUnits();
      if (!units.isEmpty()
          && units.stream().map(Unit::getOwner).allMatch(Matches.isAllied(currentPlayer))) {
        return ProUtils.getEnemyPlayersInTurnOrder(currentPlayer).stream()
            .findFirst()
            .orElseThrow();
      }
    }
    return currentPlayer;
  }

  /**
   * Returns the "best" defender, i.e. an enemy of the current player. If possible, prefers enemies
   * in the given territory with the most units.
   *
   * @return best defender
   */
  @Nullable
  private GamePlayer getBestDefender(final GamePlayer attacker) {
    assert currentPlayer
        != null; // should not occur checked in calling method getAttackerAndDefender

    final List<GamePlayer> potentialDefenders = ProUtils.getEnemyPlayers(attacker);
    if (potentialDefenders.isEmpty()) {
      return null;
    }
    // determine next player after current player who could be a defender
    final GamePlayer nextPlayerAsDefender =
        ProUtils.getOtherPlayersInTurnOrder(attacker).stream()
            .filter(potentialDefenders::contains)
            .findFirst()
            .orElseThrow();

    if (territory == null) {
      // Without territory, we cannot prioritize defender by number of units
      return nextPlayerAsDefender;
    }

    return getBestDefenderFromTerritory(potentialDefenders, nextPlayerAsDefender);
  }

  private GamePlayer getBestDefenderFromTerritory(
      final List<GamePlayer> potentialDefenders, final GamePlayer nextPlayerAsDefender) {
    // Select the defender to be an enemy of the current player, if possible, prefer enemies
    // in the given territory.
    assert territory != null; // should not occur as checked in calling method getBestDefender

    final GamePlayer territoryOwner = territory.getOwner();
    if (!territoryOwner.isNull() && potentialDefenders.contains(territoryOwner)) {
      // In case the owner is a potential defender and has units in the land it's the defender
      if (territory.getUnits().stream()
          .map(Unit::getOwner)
          .filter(territoryOwner::equals)
          .findAny()
          .isPresent()) {
        return territoryOwner;
      }
    }

    // When deciding for an enemy, usually a player having more units is
    // more important and more likely to be meant, e.g. a territory with 10 units of player A
    // and 1 unit of player B. Thus, we use lists and ordered streams.
    final List<GamePlayer> sortedPlayersForDefender =
        territory.getUnitCollection().getPlayersSortedByUnitCount();

    // Add the territory owner at the end of the priority list. This way, when attacking an
    // empty territory, the owner gets preferred even though they have no units in their land.
    sortedPlayersForDefender.add(territoryOwner);

    sortedPlayersForDefender.removeIf(p -> !potentialDefenders.contains(p));
    sortedPlayersForDefender.add(nextPlayerAsDefender);

    // Attacker fights alone for this selector; the defender can also use all the allied units.
    return sortedPlayersForDefender.get(0);
  }
}
