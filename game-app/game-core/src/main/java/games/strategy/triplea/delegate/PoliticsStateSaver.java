package games.strategy.triplea.delegate;

import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@link PoliticsExtendedDelegateState}. The state carries only its {@code
 * superState} chain, delegated to {@link SaverSupport#writeNested} (native once a saver exists for
 * it, a legacy blob until then).
 */
public final class PoliticsStateSaver implements TextSaver<PoliticsExtendedDelegateState> {

  @Override
  public Class<PoliticsExtendedDelegateState> type() {
    return PoliticsExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(final PoliticsExtendedDelegateState state, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(state.superState, refs));
    return json;
  }

  @Override
  public PoliticsExtendedDelegateState read(final JsonObject json, final GameRefResolver refs) {
    final PoliticsExtendedDelegateState state = new PoliticsExtendedDelegateState();
    state.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    return state;
  }
}
