package games.strategy.engine.data;

import games.strategy.triplea.attachments.RelationshipTypeAttachment;

/** A type of relationship (e.g. allied, at war, etc.) available between players. */
public class RelationshipType extends NamedAttachable {
  private static final long serialVersionUID = 5348310616624709971L;

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
