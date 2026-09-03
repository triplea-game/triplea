package games.strategy.triplea.delegate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Text serializer for {@link TechnologyExtendedDelegateState}. Lives in this package because the
 * state class and its fields are package-private. The {@code techs} map references each {@link
 * GamePlayer} key and each {@link TechAdvance} value by name via {@link GameRefResolver}, never by
 * value — a null map is preserved distinctly from an empty one via {@link JsonNull}.
 *
 * <p>Covers {@code TechnologyDelegate}.
 */
public final class TechnologyStateSaver implements TextSaver<TechnologyExtendedDelegateState> {

  @Override
  public Class<TechnologyExtendedDelegateState> type() {
    return TechnologyExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(final TechnologyExtendedDelegateState state, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(state.superState, refs));
    json.addProperty("needToInitialize", state.needToInitialize);
    if (state.techs == null) {
      json.add("techs", JsonNull.INSTANCE);
    } else {
      final JsonArray techsArr = new JsonArray();
      for (final Map.Entry<GamePlayer, Collection<TechAdvance>> entry : state.techs.entrySet()) {
        final JsonObject techEntry = new JsonObject();
        techEntry.add("player", refs.writeRef(entry.getKey()));
        techEntry.add("techs", SaverSupport.writeRefList(entry.getValue(), refs));
        techsArr.add(techEntry);
      }
      json.add("techs", techsArr);
    }
    return json;
  }

  @Override
  public TechnologyExtendedDelegateState read(final JsonObject json, final GameRefResolver refs) {
    final TechnologyExtendedDelegateState state = new TechnologyExtendedDelegateState();
    state.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    state.needToInitialize = json.get("needToInitialize").getAsBoolean();
    final JsonElement techs = json.get("techs");
    if (techs == null || techs.isJsonNull()) {
      state.techs = null;
    } else {
      final Map<GamePlayer, Collection<TechAdvance>> map = new HashMap<>();
      for (final JsonElement element : techs.getAsJsonArray()) {
        final JsonObject techEntry = element.getAsJsonObject();
        final GamePlayer player = (GamePlayer) refs.readRef(techEntry.getAsJsonObject("player"));
        map.put(
            player,
            SaverSupport.readRefList(techEntry.getAsJsonArray("techs"), refs, TechAdvance.class));
      }
      state.techs = map;
    }
    return state;
  }
}
