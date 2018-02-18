package games.strategy.engine.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import games.strategy.triplea.delegate.Matches;

public class RelationshipInterpreter extends GameDataComponent {
  private static final long serialVersionUID = -643454441052535241L;

  public RelationshipInterpreter(final GameData data) {
    super(data);
  }

  /**
   * @param p1
   *        first referring player
   * @param p2
   *        second referring player
   * @return whether player p1 is allied to player p2.
   */
  public boolean isAllied(final PlayerID p1, final PlayerID p2) {
    return Matches.relationshipTypeIsAllied().test((getRelationshipType(p1, p2)));
  }

  public boolean isAlliedWithAnyOfThesePlayers(final PlayerID p1, final Collection<PlayerID> p2s) {
    for (final PlayerID p2 : p2s) {
      if (Matches.relationshipTypeIsAllied().test((getRelationshipType(p1, p2)))) {
        return true;
      }
    }
    return false;
  }

  public Set<PlayerID> getAllies(final PlayerID p1, final boolean includeSelf) {
    final Set<PlayerID> allies = new HashSet<>();
    for (final PlayerID player : getData().getPlayerList().getPlayers()) {
      if (Matches.relationshipTypeIsAllied().test(getRelationshipType(p1, player))) {
        allies.add(player);
      }
    }
    if (includeSelf) {
      allies.add(p1);
    } else {
      allies.remove(p1);
    }
    return allies;
  }

  /**
   * returns true if p1 is at war with p2.
   *
   * @param p1
   *        player1
   * @param p2
   *        player2
   * @return whether p1 is at war with p2
   */
  public boolean isAtWar(final PlayerID p1, final PlayerID p2) {
    return Matches.relationshipTypeIsAtWar().test((getRelationshipType(p1, p2)));
  }

  public boolean isAtWarWithAnyOfThesePlayers(final PlayerID p1, final Collection<PlayerID> p2s) {
    for (final PlayerID p2 : p2s) {
      if (Matches.relationshipTypeIsAtWar().test((getRelationshipType(p1, p2)))) {
        return true;
      }
    }
    return false;
  }

  public Set<PlayerID> getEnemies(final PlayerID p1) {
    final Set<PlayerID> enemies = new HashSet<>();
    for (final PlayerID player : getData().getPlayerList().getPlayers()) {
      if (Matches.relationshipTypeIsAtWar().test(getRelationshipType(p1, player))) {
        enemies.add(player);
      }
    }
    enemies.remove(p1);
    return enemies;
  }

  /**
   * @param p1
   *        player1
   * @param p2
   *        player2
   * @return whether player1 is neutral to player2.
   */
  public boolean isNeutral(final PlayerID p1, final PlayerID p2) {
    return Matches.relationshipTypeIsNeutral().test((getRelationshipType(p1, p2)));
  }

  public boolean isNeutralWithAnyOfThesePlayers(final PlayerID p1, final Collection<PlayerID> p2s) {
    for (final PlayerID p2 : p2s) {
      if (Matches.relationshipTypeIsNeutral().test((getRelationshipType(p1, p2)))) {
        return true;
      }
    }
    return false;
  }

  public boolean canMoveLandUnitsOverOwnedLand(final PlayerID p1, final PlayerID p2) {
    return Matches.relationshipTypeCanMoveLandUnitsOverOwnedLand().test(getRelationshipType(p1, p2));
  }

  public boolean canMoveAirUnitsOverOwnedLand(final PlayerID p1, final PlayerID p2) {
    return Matches.relationshipTypeCanMoveAirUnitsOverOwnedLand().test(getRelationshipType(p1, p2));
  }

  public boolean canLandAirUnitsOnOwnedLand(final PlayerID p1, final PlayerID p2) {
    return Matches.relationshipTypeCanLandAirUnitsOnOwnedLand().test(getRelationshipType(p1, p2));
  }

  public String getUpkeepCost(final PlayerID p1, final PlayerID p2) {
    return getRelationshipType(p1, p2).getRelationshipTypeAttachment().getUpkeepCost();
  }

  public boolean alliancesCanChainTogether(final PlayerID p1, final PlayerID p2) {
    return Matches.relationshipTypeIsAlliedAndAlliancesCanChainTogether().test(getRelationshipType(p1, p2));
  }

  public boolean isDefaultWarPosition(final PlayerID p1, final PlayerID p2) {
    return Matches.relationshipTypeIsDefaultWarPosition().test(getRelationshipType(p1, p2));
  }

  public boolean canTakeOverOwnedTerritory(final PlayerID p1, final PlayerID p2) {
    return Matches.relationshipTypeCanTakeOverOwnedTerritory().test(getRelationshipType(p1, p2));
  }

  public boolean givesBackOriginalTerritories(final PlayerID p1, final PlayerID p2) {
    return Matches.relationshipTypeGivesBackOriginalTerritories().test(getRelationshipType(p1, p2));
  }

  public boolean canMoveIntoDuringCombatMove(final PlayerID p1, final PlayerID p2) {
    return Matches.relationshipTypeCanMoveIntoDuringCombatMove().test(getRelationshipType(p1, p2));
  }

  public boolean canMoveThroughCanals(final PlayerID p1, final PlayerID p2) {
    return Matches.relationshipTypeCanMoveThroughCanals().test(getRelationshipType(p1, p2));
  }

  public boolean rocketsCanFlyOver(final PlayerID p1, final PlayerID p2) {
    return Matches.relationshipTypeRocketsCanFlyOver().test(getRelationshipType(p1, p2));
  }

  /**
   * Convenience method to get RelationshipType so you can do relationshipChecks on the relationship between these 2
   * players.
   *
   * @param p1
   *        Player1 in the relationship
   * @param p2
   *        Player2 in the relationship
   * @return the current RelationshipType between those two players
   */
  RelationshipType getRelationshipType(final PlayerID p1, final PlayerID p2) {
    return getData().getRelationshipTracker().getRelationshipType(p1, p2);
  }
}
