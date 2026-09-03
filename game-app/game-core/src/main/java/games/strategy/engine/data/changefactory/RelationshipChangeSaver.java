package games.strategy.engine.data.changefactory;

import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.Reflect;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@code RelationshipChange} (two players and two relationship types by name).
 */
public final class RelationshipChangeSaver implements TextSaver<RelationshipChange> {

  @Override
  public Class<RelationshipChange> type() {
    return RelationshipChange.class;
  }

  @Override
  public JsonObject write(final RelationshipChange change, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.addProperty("player1Name", (String) Reflect.get(change, "player1Name"));
    json.addProperty("player2Name", (String) Reflect.get(change, "player2Name"));
    json.addProperty(
        "oldRelationshipTypeName", (String) Reflect.get(change, "oldRelationshipTypeName"));
    json.addProperty(
        "newRelationshipTypeName", (String) Reflect.get(change, "newRelationshipTypeName"));
    return json;
  }

  @Override
  public RelationshipChange read(final JsonObject json, final GameRefResolver refs) {
    return Reflect.construct(
        RelationshipChange.class,
        new Class<?>[] {String.class, String.class, String.class, String.class},
        json.get("player1Name").getAsString(),
        json.get("player2Name").getAsString(),
        json.get("oldRelationshipTypeName").getAsString(),
        json.get("newRelationshipTypeName").getAsString());
  }
}
