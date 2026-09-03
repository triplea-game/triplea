package games.strategy.engine.data.changefactory;

import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.Reflect;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@code ChangeResourceChange} (player + resource by name, signed quantity).
 */
public final class ChangeResourceChangeSaver implements TextSaver<ChangeResourceChange> {

  @Override
  public Class<ChangeResourceChange> type() {
    return ChangeResourceChange.class;
  }

  @Override
  public JsonObject write(final ChangeResourceChange change, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.addProperty("playerName", (String) Reflect.get(change, "playerName"));
    json.addProperty("resourceName", (String) Reflect.get(change, "resourceName"));
    json.addProperty("quantity", (Integer) Reflect.get(change, "quantity"));
    return json;
  }

  @Override
  public ChangeResourceChange read(final JsonObject json, final GameRefResolver refs) {
    return Reflect.construct(
        ChangeResourceChange.class,
        new Class<?>[] {String.class, String.class, int.class},
        json.get("playerName").getAsString(),
        json.get("resourceName").getAsString(),
        json.get("quantity").getAsInt());
  }
}
