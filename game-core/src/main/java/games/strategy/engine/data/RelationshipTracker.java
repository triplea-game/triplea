package games.strategy.engine.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import games.strategy.triplea.attachments.RelationshipTypeAttachment;

/** A collection of relationships between any two players. */
public class RelationshipTracker extends RelationshipInterpreter {
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
      final PlayerId p1, final PlayerId p2, final RelationshipType relationshipType) {
    relationships.put(new RelatedPlayers(p1, p2), new Relationship(relationshipType));
  }

  /**
   * Method for setting a relationship between two players, this should only be called during the
   * Game Parser.
   */
  protected void setRelationship(
      final PlayerId p1, final PlayerId p2, final RelationshipType r, final int roundValue) {
    relationships.put(new RelatedPlayers(p1, p2), new Relationship(r, roundValue));
  }

  @Override
  public RelationshipType getRelationshipType(final PlayerId p1, final PlayerId p2) {
    return getRelationship(p1, p2).getRelationshipType();
  }

  public Relationship getRelationship(final PlayerId p1, final PlayerId p2) {
    return relationships.get(new RelatedPlayers(p1, p2));
  }

  public Set<Relationship> getRelationships(final PlayerId player1) {
    final Set<Relationship> relationships = new HashSet<>();
    for (final PlayerId player2 : getData().getPlayerList().getPlayers()) {
      if (player2 == null || player2.equals(player1)) {
        continue;
      }
      relationships.add(getRelationship(player1, player2));
    }
    return relationships;
  }

  public int getRoundRelationshipWasCreated(final PlayerId p1, final PlayerId p2) {
    return relationships.get(new RelatedPlayers(p1, p2)).getRoundCreated();
  }

  /**
   * Convenience method to directly access relationshipTypeAttachment on the relationship that
   * exists between two players.
   *
   * @param p1 Player 1 in the relationship
   * @param p2 Player 2 in the relationship
   * @return the current RelationshipTypeAttachment attached to the current relationship that exists
   *     between those 2 players
   */
  protected RelationshipTypeAttachment getRelationshipTypeAttachment(
      final PlayerId p1, final PlayerId p2) {
    final RelationshipType relation = getRelationshipType(p1, p2);
    return RelationshipTypeAttachment.get(relation);
  }

  /**
   * This methods will create all SelfRelations of all players including NullPlayer with oneself.
   * This method should only be called once.
   */
  protected void setSelfRelations() {
    for (final PlayerId p : getData().getPlayerList().getPlayers()) {
      setRelationship(p, p, getSelfRelationshipType());
    }
    setRelationship(PlayerId.NULL_PLAYERID, PlayerId.NULL_PLAYERID, getSelfRelationshipType());
  }

  /**
   * This methods will create all relationship of all players with the NullPlayer. This method
   * should only be called once.
   */
  protected void setNullPlayerRelations() {
    for (final PlayerId p : getData().getPlayerList().getPlayers()) {
      setRelationship(p, PlayerId.NULL_PLAYERID, getNullRelationshipType());
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

    private final PlayerId player1;
    private final PlayerId player2;

    RelatedPlayers(final PlayerId player1, final PlayerId player2) {
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
  public class Relationship implements Serializable {
    private static final long serialVersionUID = -6718866176901627180L;

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

    private final RelationshipType relationshipType;
    private final int roundCreated;

    public int getRoundCreated() {
      return roundCreated;
    }

    public RelationshipType getRelationshipType() {
      return relationshipType;
    }

    @Override
    public String toString() {
      return roundCreated + ":" + relationshipType;
    }
  }
}
