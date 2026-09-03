package games.strategy.triplea.delegate;

import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@link EndTurnExtendedDelegateState}. Lives in this package because the state
 * class and its fields are package-private. The {@code superState} chain is delegated to {@link
 * SaverSupport#writeNested}; the remaining primitives are written directly.
 */
public final class EndTurnStateSaver implements TextSaver<EndTurnExtendedDelegateState> {

  @Override
  public Class<EndTurnExtendedDelegateState> type() {
    return EndTurnExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(final EndTurnExtendedDelegateState state, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(state.superState, refs));
    json.addProperty("needToInitialize", state.needToInitialize);
    json.addProperty("hasPostedTurnSummary", state.hasPostedTurnSummary);
    return json;
  }

  @Override
  public EndTurnExtendedDelegateState read(final JsonObject json, final GameRefResolver refs) {
    final EndTurnExtendedDelegateState state = new EndTurnExtendedDelegateState();
    state.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    state.needToInitialize = json.get("needToInitialize").getAsBoolean();
    state.hasPostedTurnSummary = json.get("hasPostedTurnSummary").getAsBoolean();
    return state;
  }
}
