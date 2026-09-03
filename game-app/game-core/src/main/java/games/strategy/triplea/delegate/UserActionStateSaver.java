package games.strategy.triplea.delegate;

import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@link UserActionExtendedDelegateState}. The state carries only its {@code
 * superState} chain, delegated to {@link SaverSupport#writeNested} (native once a saver exists for
 * it, a legacy blob until then).
 */
public final class UserActionStateSaver implements TextSaver<UserActionExtendedDelegateState> {

  @Override
  public Class<UserActionExtendedDelegateState> type() {
    return UserActionExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(final UserActionExtendedDelegateState state, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(state.superState, refs));
    return json;
  }

  @Override
  public UserActionExtendedDelegateState read(final JsonObject json, final GameRefResolver refs) {
    final UserActionExtendedDelegateState state = new UserActionExtendedDelegateState();
    state.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    return state;
  }
}
