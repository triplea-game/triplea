package games.strategy.triplea.delegate;

import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@link BidPurchaseExtendedDelegateState}. Lives in this package because the
 * state class and its fields are package-private. The {@code superState} chain is delegated to
 * {@link SaverSupport#writeNested}; the remaining primitives are written directly.
 */
public final class BidPurchaseStateSaver implements TextSaver<BidPurchaseExtendedDelegateState> {

  @Override
  public Class<BidPurchaseExtendedDelegateState> type() {
    return BidPurchaseExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(
      final BidPurchaseExtendedDelegateState state, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(state.superState, refs));
    json.addProperty("bid", state.bid);
    json.addProperty("spent", state.spent);
    json.addProperty("hasBid", state.hasBid);
    return json;
  }

  @Override
  public BidPurchaseExtendedDelegateState read(final JsonObject json, final GameRefResolver refs) {
    final BidPurchaseExtendedDelegateState state = new BidPurchaseExtendedDelegateState();
    state.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    state.bid = json.get("bid").getAsInt();
    state.spent = json.get("spent").getAsInt();
    state.hasBid = json.get("hasBid").getAsBoolean();
    return state;
  }
}
