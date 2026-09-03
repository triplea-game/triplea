package games.strategy.triplea.delegate;

import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@code SpecialMoveExtendedDelegateState} (paratroop/airborne moves). Its own
 * state is a single flag; the {@code superState} (an {@code AbstractMoveExtendedDelegateState} with
 * the undo stack and any in-flight {@code MovePerformer}) rides through {@link
 * SaverSupport#writeNested} — natively once a saver exists for it, a legacy blob until then. That
 * blob is exactly the v0 save-boundary: active move resolution is not yet text-encoded.
 */
public final class SpecialMoveStateSaver implements TextSaver<SpecialMoveExtendedDelegateState> {

  @Override
  public Class<SpecialMoveExtendedDelegateState> type() {
    return SpecialMoveExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(final SpecialMoveExtendedDelegateState s, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(s.superState, refs));
    json.addProperty("needToInitialize", s.needToInitialize);
    return json;
  }

  @Override
  public SpecialMoveExtendedDelegateState read(final JsonObject json, final GameRefResolver refs) {
    final SpecialMoveExtendedDelegateState s = new SpecialMoveExtendedDelegateState();
    s.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    s.needToInitialize = json.get("needToInitialize").getAsBoolean();
    return s;
  }
}
