package games.strategy.triplea.delegate;

import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@link BaseDelegateState} — the root delegate state. It has no {@code
 * superState}; only the two step-tracking booleans, written directly.
 */
public final class BaseDelegateStateSaver implements TextSaver<BaseDelegateState> {

  @Override
  public Class<BaseDelegateState> type() {
    return BaseDelegateState.class;
  }

  @Override
  public JsonObject write(final BaseDelegateState state, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.addProperty("startBaseStepsFinished", state.startBaseStepsFinished);
    json.addProperty("endBaseStepsFinished", state.endBaseStepsFinished);
    return json;
  }

  @Override
  public BaseDelegateState read(final JsonObject json, final GameRefResolver refs) {
    final BaseDelegateState state = new BaseDelegateState();
    state.startBaseStepsFinished = json.get("startBaseStepsFinished").getAsBoolean();
    state.endBaseStepsFinished = json.get("endBaseStepsFinished").getAsBoolean();
    return state;
  }
}
