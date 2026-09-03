package games.strategy.engine.data.changefactory;

import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.Reflect;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@code PlayerWhoAmIChange} (player name + old/new encoded whoAmI strings).
 */
public final class PlayerWhoAmIChangeSaver implements TextSaver<PlayerWhoAmIChange> {

  @Override
  public Class<PlayerWhoAmIChange> type() {
    return PlayerWhoAmIChange.class;
  }

  @Override
  public JsonObject write(final PlayerWhoAmIChange change, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.addProperty("startWhoAmI", (String) Reflect.get(change, "startWhoAmI"));
    json.addProperty("endWhoAmI", (String) Reflect.get(change, "endWhoAmI"));
    json.addProperty("playerName", (String) Reflect.get(change, "playerName"));
    return json;
  }

  @Override
  public PlayerWhoAmIChange read(final JsonObject json, final GameRefResolver refs) {
    return Reflect.construct(
        PlayerWhoAmIChange.class,
        new Class<?>[] {String.class, String.class, String.class},
        json.get("startWhoAmI").getAsString(),
        json.get("endWhoAmI").getAsString(),
        json.get("playerName").getAsString());
  }
}
