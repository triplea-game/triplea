package games.strategy.triplea.delegate;

import com.google.gson.JsonObject;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@link EndRoundExtendedDelegateState}. Lives in this package because the
 * state class and its fields are package-private. The {@code winners} collection references each
 * {@link GamePlayer} by name via {@link SaverSupport#writeRefList}, never by value.
 *
 * <p>Covers {@code EndRoundDelegate}.
 */
public final class EndRoundStateSaver implements TextSaver<EndRoundExtendedDelegateState> {

  @Override
  public Class<EndRoundExtendedDelegateState> type() {
    return EndRoundExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(final EndRoundExtendedDelegateState state, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(state.superState, refs));
    json.addProperty("gameOver", state.gameOver);
    json.add("winners", SaverSupport.writeRefList(state.winners, refs));
    return json;
  }

  @Override
  public EndRoundExtendedDelegateState read(final JsonObject json, final GameRefResolver refs) {
    final EndRoundExtendedDelegateState state = new EndRoundExtendedDelegateState();
    state.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    state.gameOver = json.get("gameOver").getAsBoolean();
    state.winners =
        SaverSupport.readRefList(json.getAsJsonArray("winners"), refs, GamePlayer.class);
    return state;
  }
}
