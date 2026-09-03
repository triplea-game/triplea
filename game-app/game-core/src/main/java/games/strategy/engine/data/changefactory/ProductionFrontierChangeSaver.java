package games.strategy.engine.data.changefactory;

import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.Reflect;
import games.strategy.engine.data.serializer.TextSaver;

/** Text serializer for {@code ProductionFrontierChange} (player + old/new frontier names). */
public final class ProductionFrontierChangeSaver implements TextSaver<ProductionFrontierChange> {

  @Override
  public Class<ProductionFrontierChange> type() {
    return ProductionFrontierChange.class;
  }

  @Override
  public JsonObject write(final ProductionFrontierChange change, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.addProperty("startFrontierName", (String) Reflect.get(change, "startFrontierName"));
    json.addProperty("endFrontierName", (String) Reflect.get(change, "endFrontierName"));
    json.addProperty("playerName", (String) Reflect.get(change, "playerName"));
    return json;
  }

  @Override
  public ProductionFrontierChange read(final JsonObject json, final GameRefResolver refs) {
    return Reflect.construct(
        ProductionFrontierChange.class,
        new Class<?>[] {String.class, String.class, String.class},
        json.get("startFrontierName").getAsString(),
        json.get("endFrontierName").getAsString(),
        json.get("playerName").getAsString());
  }
}
