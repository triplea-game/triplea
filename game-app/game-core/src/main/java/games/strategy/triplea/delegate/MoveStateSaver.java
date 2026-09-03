package games.strategy.triplea.delegate;

import com.google.gson.JsonObject;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;

/**
 * Text serializer for {@code MoveExtendedDelegateState}. Its own fields (two flags and an {@code
 * IntegerMap<Territory>} of PUs lost) are written natively; the {@code superState} (an {@code
 * AbstractMoveExtendedDelegateState} carrying the undo stack and any in-flight {@code
 * MovePerformer}) rides through {@link SaverSupport#writeNested} — a legacy blob until that graph
 * is text-encoded (the v0 save-boundary).
 */
public final class MoveStateSaver implements TextSaver<MoveExtendedDelegateState> {

  @Override
  public Class<MoveExtendedDelegateState> type() {
    return MoveExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(final MoveExtendedDelegateState s, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(s.superState, refs));
    json.addProperty("needToInitialize", s.needToInitialize);
    json.addProperty("needToDoRockets", s.needToDoRockets);
    json.add("pusLost", SaverSupport.writeEntityIntegerMap(s.pusLost, refs));
    return json;
  }

  @Override
  public MoveExtendedDelegateState read(final JsonObject json, final GameRefResolver refs) {
    final MoveExtendedDelegateState s = new MoveExtendedDelegateState();
    s.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    s.needToInitialize = json.get("needToInitialize").getAsBoolean();
    s.needToDoRockets = json.get("needToDoRockets").getAsBoolean();
    s.pusLost =
        SaverSupport.readEntityIntegerMap(json.getAsJsonArray("pusLost"), refs, Territory.class);
    return s;
  }
}
