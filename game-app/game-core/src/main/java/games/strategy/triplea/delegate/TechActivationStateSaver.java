package games.strategy.triplea.delegate;

import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@link TechActivationExtendedDelegateState}. Lives in this package because
 * the state class and its fields are package-private. The {@code superState} chain is delegated to
 * {@link SaverSupport#writeNested}; the remaining primitive is written directly.
 */
public final class TechActivationStateSaver
    implements TextSaver<TechActivationExtendedDelegateState> {

  @Override
  public Class<TechActivationExtendedDelegateState> type() {
    return TechActivationExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(
      final TechActivationExtendedDelegateState state, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(state.superState, refs));
    json.addProperty("needToInitialize", state.needToInitialize);
    return json;
  }

  @Override
  public TechActivationExtendedDelegateState read(
      final JsonObject json, final GameRefResolver refs) {
    final TechActivationExtendedDelegateState state = new TechActivationExtendedDelegateState();
    state.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    state.needToInitialize = json.get("needToInitialize").getAsBoolean();
    return state;
  }
}
