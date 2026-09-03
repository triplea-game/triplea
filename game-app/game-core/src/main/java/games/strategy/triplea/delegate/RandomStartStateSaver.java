package games.strategy.triplea.delegate;

import com.google.gson.JsonObject;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@link RandomStartExtendedDelegateState}. Beyond the {@code superState} chain
 * it carries a nullable {@code currentPickingPlayer}, referenced by name via {@link
 * SaverSupport#writeRefOrNull} so null is preserved distinctly from a real player.
 */
public final class RandomStartStateSaver implements TextSaver<RandomStartExtendedDelegateState> {

  @Override
  public Class<RandomStartExtendedDelegateState> type() {
    return RandomStartExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(
      final RandomStartExtendedDelegateState state, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(state.superState, refs));
    json.add("currentPickingPlayer", SaverSupport.writeRefOrNull(state.currentPickingPlayer, refs));
    return json;
  }

  @Override
  public RandomStartExtendedDelegateState read(final JsonObject json, final GameRefResolver refs) {
    final RandomStartExtendedDelegateState state = new RandomStartExtendedDelegateState();
    state.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    state.currentPickingPlayer =
        SaverSupport.readRefOrNull(json.get("currentPickingPlayer"), refs, GamePlayer.class);
    return state;
  }
}
