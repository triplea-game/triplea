package games.strategy.engine.data;

import games.strategy.triplea.attachments.RelationshipTypeAttachment;

public class RelationshipType extends NamedAttachable {
  private static final long serialVersionUID = 5348310616624709971L;

  /**
   * create new RelationshipType.
   *
   * @param name
   *        name of the relationshipType
   * @param data
   *        GameData Object used for construction
   */
  public RelationshipType(final String name, final GameData data) {
    super(name, data);
  }

  /**
   * convenience method to get the relationshipTypeAttachment of this relationshipType.
   *
   * @return the relationshipTypeAttachment of this relationshipType
   */
  public RelationshipTypeAttachment getRelationshipTypeAttachment() {
    return RelationshipTypeAttachment.get(this);
  }

  @Override
  public String toString() {
    return this.getName();
  }
}
