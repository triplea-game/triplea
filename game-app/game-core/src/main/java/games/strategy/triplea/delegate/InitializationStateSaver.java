package games.strategy.triplea.delegate;

import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@link InitializationExtendedDelegateState}. Lives in this package because
 * the state class and its fields are package-private. The {@code superState} chain is delegated to
 * {@link SaverSupport#writeNested}; the remaining primitive is written directly.
 */
public final class InitializationStateSaver
    implements TextSaver<InitializationExtendedDelegateState> {

  @Override
  public Class<InitializationExtendedDelegateState> type() {
    return InitializationExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(
      final InitializationExtendedDelegateState state, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(state.superState, refs));
    json.addProperty("needToInitialize", state.needToInitialize);
    return json;
  }

  @Override
  public InitializationExtendedDelegateState read(
      final JsonObject json, final GameRefResolver refs) {
    final InitializationExtendedDelegateState state = new InitializationExtendedDelegateState();
    state.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    state.needToInitialize = json.get("needToInitialize").getAsBoolean();
    return state;
  }
}
