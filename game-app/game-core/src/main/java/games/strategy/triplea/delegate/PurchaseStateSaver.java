package games.strategy.triplea.delegate;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@link PurchaseExtendedDelegateState} — the reference implementation every
 * FLAT delegate-state saver copies. It lives in this package because the state class and its fields
 * are package-private. Note the shape:
 *
 * <ul>
 *   <li>the {@code superState} chain is delegated to {@link SaverSupport#writeNested} (native once
 *       a saver exists for it, a legacy blob until then);
 *   <li>primitives are written directly;
 *   <li>the {@code IntegerMap<ProductionRule>} references each rule by name via {@link
 *       GameRefResolver}, never by value — null is preserved distinctly from empty.
 * </ul>
 *
 * <p>Covers {@code PurchaseDelegate} and {@code NoPuPurchaseDelegate} (both use this state).
 */
public final class PurchaseStateSaver implements TextSaver<PurchaseExtendedDelegateState> {

  @Override
  public Class<PurchaseExtendedDelegateState> type() {
    return PurchaseExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(final PurchaseExtendedDelegateState state, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(state.superState, refs));
    json.addProperty("needToInitialize", state.needToInitialize);
    json.add("pendingProductionRules", writeRules(state, refs));
    return json;
  }

  @Override
  public PurchaseExtendedDelegateState read(final JsonObject json, final GameRefResolver refs) {
    final PurchaseExtendedDelegateState state = new PurchaseExtendedDelegateState();
    state.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    state.needToInitialize = json.get("needToInitialize").getAsBoolean();
    final JsonElement rules = json.get("pendingProductionRules");
    state.pendingProductionRules =
        rules == null || rules.isJsonNull()
            ? null
            : SaverSupport.readEntityIntegerMap(rules.getAsJsonArray(), refs, ProductionRule.class);
    return state;
  }

  private static JsonElement writeRules(
      final PurchaseExtendedDelegateState state, final GameRefResolver refs) {
    return state.pendingProductionRules == null
        ? JsonNull.INSTANCE
        : SaverSupport.writeEntityIntegerMap(state.pendingProductionRules, refs);
  }
}
