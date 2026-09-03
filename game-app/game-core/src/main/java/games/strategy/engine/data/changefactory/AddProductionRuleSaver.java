package games.strategy.engine.data.changefactory;

import com.google.gson.JsonObject;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.Reflect;
import games.strategy.engine.data.serializer.TextSaver;

/** Text serializer for {@code AddProductionRule} (a production rule and frontier, both by name). */
public final class AddProductionRuleSaver implements TextSaver<AddProductionRule> {

  @Override
  public Class<AddProductionRule> type() {
    return AddProductionRule.class;
  }

  @Override
  public JsonObject write(final AddProductionRule change, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("rule", refs.writeRef(Reflect.get(change, "rule")));
    json.add("frontier", refs.writeRef(Reflect.get(change, "frontier")));
    return json;
  }

  @Override
  public AddProductionRule read(final JsonObject json, final GameRefResolver refs) {
    final ProductionRule rule = (ProductionRule) refs.readRef(json.getAsJsonObject("rule"));
    final ProductionFrontier frontier =
        (ProductionFrontier) refs.readRef(json.getAsJsonObject("frontier"));
    return new AddProductionRule(rule, frontier);
  }
}
