package games.strategy.engine.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A collection of Relationship types
 */
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.RelationshipTypeAttachment;

public class RelationshipTypeList extends GameDataComponent implements Iterable<RelationshipType> {
  private static final long serialVersionUID = 6590541694575435151L;
  private final HashMap<String, RelationshipType> m_relationshipTypes = new HashMap<String, RelationshipType>();

  /**
   * convenience method to return the RELATIONSHIP_TYPE_SELF relation (the relation you have with yourself)
   *
   * @return the relation one has with oneself.
   */
  public RelationshipType getSelfRelation() {
    return this.getRelationshipType(Constants.RELATIONSHIP_TYPE_SELF);
  }

  /**
   * convenience method to return the RELATIONSHIP_TYPE_NULL relation (the relation you have with the Neutral Player)
   *
   * @return the relation one has with the Neutral.
   */
  public RelationshipType getNullRelation() {
    return this.getRelationshipType(Constants.RELATIONSHIP_TYPE_NULL);
  }

  /**
   * Constructs a new RelationshipTypeList
   *
   * @param data
   *        GameData used for construction
   * @throws GameParseException
   */
  protected RelationshipTypeList(final GameData data) {
    super(data);
    try {
      createDefaultRelationship(Constants.RELATIONSHIP_TYPE_SELF, RelationshipTypeAttachment.ARCHETYPE_ALLIED, data);
      createDefaultRelationship(Constants.RELATIONSHIP_TYPE_NULL, RelationshipTypeAttachment.ARCHETYPE_WAR, data);
      createDefaultRelationship(Constants.RELATIONSHIP_TYPE_DEFAULT_WAR, RelationshipTypeAttachment.ARCHETYPE_WAR,
          data);
      createDefaultRelationship(Constants.RELATIONSHIP_TYPE_DEFAULT_ALLIED, RelationshipTypeAttachment.ARCHETYPE_ALLIED,
          data);
    } catch (final GameParseException e) {
      // this should never happen, createDefaultRelationship only throws a GameParseException when the wrong ArcheType
      // is supplied, but we
      // never do that
      throw new IllegalStateException(e);
    }
  }

  /**
   * Creates a default relationship
   *
   * @param relationshipTypeConstant
   *        the type of relationship
   * @param relationshipArcheType
   *        the archetype of the relationship
   * @param data
   *        the GameData object for this relationship
   * @throws GameParseException
   *         if the wrong relationshipArcheType is used
   */
  private void createDefaultRelationship(final String relationshipTypeConstant, final String relationshipArcheType,
      final GameData data) throws GameParseException {
    // create a new relationshipType with the name from the constant
    final RelationshipType relationshipType = new RelationshipType(relationshipTypeConstant, data);
    // create a new attachment to attach to this type
    final RelationshipTypeAttachment at =
        new RelationshipTypeAttachment(Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME, relationshipType, data);
    at.setArcheType(relationshipArcheType);
    // attach this attachment to this type
    relationshipType.addAttachment(Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME, at);
    addRelationshipType(relationshipType);
  }

  /**
   * adds a new RelationshipType, this should only be called by the GameParser.
   *
   * @param p
   *        RelationshipType
   * @return the RelationshipType just created (convenience method for the GameParser)
   */
  protected RelationshipType addRelationshipType(final RelationshipType p) {
    m_relationshipTypes.put(p.getName(), p);
    return p;
  }

  /**
   * Gets a relationshipType from the list by name;
   *
   * @param name
   *        name of the relationshipType
   * @return RelationshipType with this name
   */
  public RelationshipType getRelationshipType(final String name) {
    return m_relationshipTypes.get(name);
  }

  /**
   * returns a relationshipTypeIterator
   */
  @Override
  public Iterator<RelationshipType> iterator() {
    return m_relationshipTypes.values().iterator();
  }

  /**
   * @return site of the relationshipTypeList, be aware that the standard size = 4 (Allied, War, Self and Null Relation)
   */
  public int size() {
    return m_relationshipTypes.size();
  }

  public RelationshipType getDefaultAlliedRelationship() {
    return this.getRelationshipType(Constants.RELATIONSHIP_TYPE_DEFAULT_ALLIED);
  }

  public RelationshipType getDefaultWarRelationship() {
    return this.getRelationshipType(Constants.RELATIONSHIP_TYPE_DEFAULT_WAR);
  }

  public Collection<RelationshipType> getAllRelationshipTypes() {
    return m_relationshipTypes.values();
  }
}
