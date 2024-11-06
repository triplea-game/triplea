package games.strategy.engine.data;

import games.strategy.triplea.delegate.Matches;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * A collection of relationships between any two players. Provides methods that determine whether
 * various types of relationships (e.g. allied, at war, etc.) exist between two or more players.
 */
public class RelationshipTracker extends GameDataComponent {
  private static final long serialVersionUID = -4740671761925519069L;

  // map of "playername:playername" to RelationshipType that exists between those 2 players
  private final Map<RelatedPlayers, Relationship> relationships = new HashMap<>();

  public RelationshipTracker(final GameData data) {
    super(data);
  }

  /**
   * Method for setting a relationship between two players, this should only be called through the
   * Change Factory.
   *
   * @param p1 Player1 that will get the relationship
   * @param p2 Player2 that will get the relationship
   * @param relationshipType the RelationshipType between those two players that will be set.
   */
  public void setRelationship(
      final GamePlayer p1, final GamePlayer p2, final RelationshipType relationshipType) {
    relationships.put(new RelatedPlayers(p1, p2), new Relationship(relationshipType));
  }

  /**
   * Method for setting a relationship between two players, this should only be called during the
   * Game Parser.
   */
  public void setRelationship(
      final GamePlayer p1, final GamePlayer p2, final RelationshipType r, final int roundValue) {
    relationships.put(new RelatedPlayers(p1, p2), new Relationship(r, roundValue));
  }

  public RelationshipType getRelationshipType(final GamePlayer p1, final GamePlayer p2) {
    return getRelationship(p1, p2).getRelationshipType();
  }

  public RelationshipType getRelationshipType(final RelatedPlayers p1p2) {
    return getRelationship(p1p2).getRelationshipType();
  }

  public Relationship getRelationship(final RelatedPlayers p1p2) {
    return relationships.get(p1p2);
  }

  public Relationship getRelationship(final GamePlayer p1, final GamePlayer p2) {
    return getRelationship(new RelatedPlayers(p1, p2));
  }

  public Set<Relationship> getRelationships(final GamePlayer player1) {
    final Set<Relationship> relationships = new HashSet<>();
    for (final GamePlayer player2 : getData().getPlayerList().getPlayers()) {
      if (player2 == null || player2.equals(player1)) {
        continue;
      }
      relationships.add(getRelationship(player1, player2));
    }
    return relationships;
  }

  public int getRoundRelationshipWasCreated(final GamePlayer p1, final GamePlayer p2) {
    return relationships.get(new RelatedPlayers(p1, p2)).getRoundCreated();
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
   * This methods will create all SelfRelations of all players including NullPlayer with oneself.
   * This method should only be called once.
   */
  public void setSelfRelations() {
    for (final GamePlayer p : getData().getPlayerList().getPlayers()) {
      setRelationship(p, p, getSelfRelationshipType());
    }
    setRelationship(
        getData().getPlayerList().getNullPlayer(),
        getData().getPlayerList().getNullPlayer(),
        getSelfRelationshipType());
  }

  /**
   * This methods will create all relationship of all players with the NullPlayer. This method
   * should only be called once.
   */
  public void setNullPlayerRelations() {
    for (final GamePlayer p : getData().getPlayerList().getPlayers()) {
      setRelationship(p, getData().getPlayerList().getNullPlayer(), getNullRelationshipType());
    }
  }

  /** convenience method to get the SelfRelationshipType added for readability. */
  private RelationshipType getSelfRelationshipType() {
    return getData().getRelationshipTypeList().getSelfRelation();
  }

  /**
   * convenience method to get the NullRelationshipType (relationship with the Nullplayer) added for
   * readability.
   */
  private RelationshipType getNullRelationshipType() {
    return getData().getRelationshipTypeList().getNullRelation();
  }

  /**
   * Two players that are related; used in relationships.
   *
   * <p>This class overrides {@link #equals(Object)} and {@link #hashCode()} such that the order of
   * the players is not considered when instances of this class are used as keys in a hash
   * container. For example, if you added an entry with the key (p1, p2), you can retrieve it with
   * either the key (p1, p2) or (p2, p1).
   */
  public static final class RelatedPlayers implements Serializable {
    private static final long serialVersionUID = 2124258606502106751L;

    private final GamePlayer player1;
    private final GamePlayer player2;

    public RelatedPlayers(final GamePlayer player1, final GamePlayer player2) {
      this.player1 = player1;
      this.player2 = player2;
    }

    @Override
    public boolean equals(final Object object) {
      if (object instanceof RelatedPlayers) {
        final RelatedPlayers relatedPlayers2 = (RelatedPlayers) object;
        return (relatedPlayers2.player1.equals(player1) && relatedPlayers2.player2.equals(player2))
            || (relatedPlayers2.player2.equals(player1) && relatedPlayers2.player1.equals(player2));
      }
      return super.equals(object);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(player1) + Objects.hashCode(player2);
    }

    @Override
    public String toString() {
      return player1.getName() + "-" + player2.getName();
    }
  }

  /** Represents the establishment of a particular type of relationship within the game. */
  @Getter
  public class Relationship implements Serializable {
    private static final long serialVersionUID = -6718866176901627180L;

    private final RelationshipType relationshipType;
    private final int roundCreated;

    /** This should never be called outside of the change factory. */
    public Relationship(final RelationshipType relationshipType) {
      this.relationshipType = relationshipType;
      this.roundCreated = getData().getSequence().getRound();
    }

    /** This should never be called outside of the game parser. */
    public Relationship(final RelationshipType relationshipType, final int roundValue) {
      this.relationshipType = relationshipType;
      this.roundCreated = roundValue;
    }

    @Override
    public String toString() {
      return roundCreated + ": " + relationshipType;
    }
  }
}
