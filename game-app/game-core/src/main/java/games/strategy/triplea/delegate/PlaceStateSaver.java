package games.strategy.triplea.delegate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Text serializer for {@code PlaceExtendedDelegateState}. The {@code produced} map (territory →
 * placed units) is written natively — territories by name, units by UUID — while {@code placements}
 * (the {@code UndoablePlacement} dependency graph) rides through {@link SaverSupport#writeNested}
 * as a legacy blob until that identity graph is text-encoded.
 */
public final class PlaceStateSaver implements TextSaver<PlaceExtendedDelegateState> {

  @Override
  public Class<PlaceExtendedDelegateState> type() {
    return PlaceExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(final PlaceExtendedDelegateState s, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(s.superState, refs));
    json.add("produced", writeProduced(s.produced, refs));
    json.add("placements", SaverSupport.writeNested((Serializable) s.placements, refs));
    return json;
  }

  @Override
  public PlaceExtendedDelegateState read(final JsonObject json, final GameRefResolver refs) {
    final PlaceExtendedDelegateState s = new PlaceExtendedDelegateState();
    s.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    s.produced = readProduced(json.get("produced"), refs);
    s.placements = readPlacements(json.getAsJsonObject("placements"), refs);
    return s;
  }

  private static JsonElement writeProduced(
      final Map<Territory, Collection<Unit>> produced, final GameRefResolver refs) {
    if (produced == null) {
      return JsonNull.INSTANCE;
    }
    final JsonArray array = new JsonArray();
    for (final Map.Entry<Territory, Collection<Unit>> entry : produced.entrySet()) {
      final JsonObject element = new JsonObject();
      element.add("territory", refs.writeRef(entry.getKey()));
      element.add("units", SaverSupport.writeRefList(entry.getValue(), refs));
      array.add(element);
    }
    return array;
  }

  private static Map<Territory, Collection<Unit>> readProduced(
      final JsonElement element, final GameRefResolver refs) {
    if (element == null || element.isJsonNull()) {
      return null;
    }
    final Map<Territory, Collection<Unit>> produced = new LinkedHashMap<>();
    for (final JsonElement entryElement : element.getAsJsonArray()) {
      final JsonObject entry = entryElement.getAsJsonObject();
      final Territory territory = (Territory) refs.readRef(entry.getAsJsonObject("territory"));
      produced.put(
          territory,
          new ArrayList<>(
              SaverSupport.readRefList(entry.getAsJsonArray("units"), refs, Unit.class)));
    }
    return produced;
  }

  @SuppressWarnings("unchecked")
  private static List<UndoablePlacement> readPlacements(
      final JsonObject json, final GameRefResolver refs) {
    return (List<UndoablePlacement>) SaverSupport.readNested(json, refs);
  }
}
