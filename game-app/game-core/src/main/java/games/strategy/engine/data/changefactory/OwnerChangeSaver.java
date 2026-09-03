package games.strategy.engine.data.changefactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.Reflect;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@code OwnerChange} — the reference implementation for the {@code Change}
 * text contract (F4). {@code Change} subclasses are package-private with {@code private final}
 * fields and private constructors, so this saver lives in their package and accesses them via
 * {@link Reflect} without modifying the class. {@code OwnerChange} already stores its data as
 * entity <em>names</em> (a territory and two owners, either owner nullable), so the text form is
 * those three strings.
 */
public final class OwnerChangeSaver implements TextSaver<OwnerChange> {

  @Override
  public Class<OwnerChange> type() {
    return OwnerChange.class;
  }

  @Override
  public JsonObject write(final OwnerChange change, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.addProperty("territoryName", (String) Reflect.get(change, "territoryName"));
    json.addProperty("newOwnerName", (String) Reflect.get(change, "newOwnerName"));
    json.addProperty("oldOwnerName", (String) Reflect.get(change, "oldOwnerName"));
    return json;
  }

  @Override
  public OwnerChange read(final JsonObject json, final GameRefResolver refs) {
    return Reflect.construct(
        OwnerChange.class,
        new Class<?>[] {String.class, String.class, String.class},
        string(json, "territoryName"),
        string(json, "newOwnerName"),
        string(json, "oldOwnerName"));
  }

  private static String string(final JsonObject json, final String key) {
    final JsonElement element = json.get(key);
    return element == null || element.isJsonNull() ? null : element.getAsString();
  }
}
