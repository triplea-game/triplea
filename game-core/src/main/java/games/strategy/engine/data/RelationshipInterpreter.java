package games.strategy.engine.data;

import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides methods that determine whether various types of relationships (e.g. allied, at war,
 * etc.) exist between two or more players.
 */
public class RelationshipInterpreter extends GameDataComponent {
  private static final long serialVersionUID = -643454441052535241L;

  public RelationshipInterpreter(final GameData data) {
    super(data);
  }

  /**
   * Indicates whether player p1 is allied to player p2.
   *
   * @param p1 first referring player
   * @param p2 second referring player
   */
  public boolean isAllied(final GamePlayer p1, final GamePlayer p2) {
    return Matches.relationshipTypeIsAllied().test(getRelationshipType(p1, p2));
  }

  public boolean isAlliedWithAnyOfThesePlayers(
      final GamePlayer gamePlayer, final Collection<GamePlayer> possibleAllies) {
    return possibleAllies.stream()
        .anyMatch(
            p2 -> Matches.relationshipTypeIsAllied().test(getRelationshipType(gamePlayer, p2)));
  }

  /** Gets the set of allied players for a given player. */
  public Set<GamePlayer> getAllies(final GamePlayer gamePlayer, final boolean includeSelf) {
    final Set<GamePlayer> allies =
        getData().getPlayerList().getPlayers().stream()
            .filter(
                player ->
                    Matches.relationshipTypeIsAllied()
                        .test(getRelationshipType(gamePlayer, player)))
            .collect(Collectors.toSet());
    if (includeSelf) {
      allies.add(gamePlayer);
    } else {
      allies.remove(gamePlayer);
    }
    return allies;
  }

  /** returns true if p1 is at war with p2. */
  public boolean isAtWar(final GamePlayer p1, final GamePlayer p2) {
    return Matches.relationshipTypeIsAtWar().test(getRelationshipType(p1, p2));
  }

  public boolean isAtWarWithAnyOfThesePlayers(
      final GamePlayer p1, final Collection<GamePlayer> p2s) {
    return p2s.stream()
        .anyMatch(p2 -> Matches.relationshipTypeIsAtWar().test(getRelationshipType(p1, p2)));
  }

  public Set<GamePlayer> getEnemies(final GamePlayer p1) {
    final Set<GamePlayer> enemies =
        getData().getPlayerList().getPlayers().stream()
            .filter(
                player -> Matches.relationshipTypeIsAtWar().test(getRelationshipType(p1, player)))
            .collect(Collectors.toSet());
    enemies.remove(p1);
    return enemies;
  }

  public boolean canMoveLandUnitsOverOwnedLand(final GamePlayer p1, final GamePlayer p2) {
    return Matches.relationshipTypeCanMoveLandUnitsOverOwnedLand()
        .test(getRelationshipType(p1, p2));
  }

  public boolean canMoveAirUnitsOverOwnedLand(final GamePlayer p1, final GamePlayer p2) {
    return Matches.relationshipTypeCanMoveAirUnitsOverOwnedLand().test(getRelationshipType(p1, p2));
  }

  public boolean canLandAirUnitsOnOwnedLand(final GamePlayer p1, final GamePlayer p2) {
    return Matches.relationshipTypeCanLandAirUnitsOnOwnedLand().test(getRelationshipType(p1, p2));
  }

  public boolean canTakeOverOwnedTerritory(final GamePlayer p1, final GamePlayer p2) {
    return Matches.relationshipTypeCanTakeOverOwnedTerritory().test(getRelationshipType(p1, p2));
  }

  public boolean givesBackOriginalTerritories(final GamePlayer p1, final GamePlayer p2) {
    return Matches.relationshipTypeGivesBackOriginalTerritories().test(getRelationshipType(p1, p2));
  }

  public boolean canMoveIntoDuringCombatMove(final GamePlayer p1, final GamePlayer p2) {
    return Matches.relationshipTypeCanMoveIntoDuringCombatMove().test(getRelationshipType(p1, p2));
  }

  public boolean canMoveThroughCanals(final GamePlayer p1, final GamePlayer p2) {
    return Matches.relationshipTypeCanMoveThroughCanals().test(getRelationshipType(p1, p2));
  }

  public boolean rocketsCanFlyOver(final GamePlayer p1, final GamePlayer p2) {
    return Matches.relationshipTypeRocketsCanFlyOver().test(getRelationshipType(p1, p2));
  }

  /**
   * Convenience method to get RelationshipType so you can do relationshipChecks on the relationship
   * between these 2 players.
   *
   * @param p1 Player1 in the relationship
   * @param p2 Player2 in the relationship
   * @return the current RelationshipType between those two players
   */
  RelationshipType getRelationshipType(final GamePlayer p1, final GamePlayer p2) {
    return getData().getRelationshipTracker().getRelationshipType(p1, p2);
  }
}
