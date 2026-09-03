package games.strategy.engine.data.changefactory;

import com.google.gson.JsonObject;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.Reflect;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@code RemoveProductionRule} (a production rule and frontier, both by name).
 */
public final class RemoveProductionRuleSaver implements TextSaver<RemoveProductionRule> {

  @Override
  public Class<RemoveProductionRule> type() {
    return RemoveProductionRule.class;
  }

  @Override
  public JsonObject write(final RemoveProductionRule change, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("rule", refs.writeRef(Reflect.get(change, "rule")));
    json.add("frontier", refs.writeRef(Reflect.get(change, "frontier")));
    return json;
  }

  @Override
  public RemoveProductionRule read(final JsonObject json, final GameRefResolver refs) {
    final ProductionRule rule = (ProductionRule) refs.readRef(json.getAsJsonObject("rule"));
    final ProductionFrontier frontier =
        (ProductionFrontier) refs.readRef(json.getAsJsonObject("frontier"));
    return new RemoveProductionRule(rule, frontier);
  }
}
