package games.strategy.engine.data;

import games.strategy.triplea.attachments.RelationshipTypeAttachment;
import java.util.HashMap;
import java.util.Map;

/** A type of relationship (e.g. allied, at war, etc.) available between players. */
public class RelationshipType extends NamedAttachable {
  private static final long serialVersionUID = -2243024389101608996L;
  private static final Map<GameData, Map<String,RelationshipType>> relationshipTypes =
      new HashMap<>();

  private RelationshipType(final String name, final GameData data) {
    super(name, data);
  }

  /**
   * Returns the {@code RelationshipType} object for {@code name} and {@code data}
   * avoiding to create an object if possible (by caching used objects).
   *
   */
  public static RelationshipType get(final String name, final GameData data) {
    Map<String,RelationshipType> relationshipTypesInGame = relationshipTypes.get(data);
    if(relationshipTypesInGame == null){
      relationshipTypesInGame = new HashMap<>();
      relationshipTypes.put(data,relationshipTypesInGame);
    }

    RelationshipType type = relationshipTypesInGame.get(name);
    if(type == null){
      type = new RelationshipType(name, data);
      relationshipTypesInGame.put(name,type);
    }

    return type;
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
