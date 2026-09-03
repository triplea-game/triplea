package games.strategy.engine.framework;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;

/**
 * Shared constants and detection for the experimental text (JSONL) save format.
 *
 * <p>Layout of an (uncompressed) text save — one record per line:
 *
 * <pre>
 *   TRIPLEA-JSONL v1                                     &lt;- sentinel (not JSON)
 *   {"section":"header","gameName":..,"engineVersion":..}
 *   {"section":"gameData","encoding":"java","bytes":..}  &lt;- whole GameData graph, legacy blob
 *   {"section":"delegate","name":..,"className":..,"encoding":"text","stateClass":..,"state":{..}}
 *   {"section":"delegate",..,"encoding":"java","bytes":..}   &lt;- not-yet-migrated delegate state
 *   {"section":"delegate",..,"encoding":"none"}              &lt;- delegate with null state
 *   {"section":"end"}
 * </pre>
 *
 * <p>Both text and legacy saves are GZIP-wrapped. A legacy save's first decompressed bytes are the
 * Java serialization magic {@code 0xAC 0xED}; a text save begins with the ASCII sentinel. {@link
 * #startsWithSentinel(byte[])} distinguishes them so reads need no configuration.
 */
final class TextGameDataFormat {

  static final Gson GSON = new Gson();

  static final String SENTINEL = "TRIPLEA-JSONL v1";

  static final String SECTION_HEADER = "header";
  static final String SECTION_GAME_DATA = "gameData";
  static final String SECTION_DELEGATE = "delegate";
  static final String SECTION_END = "end";

  static final String ENCODING_TEXT = "text";
  static final String ENCODING_JAVA = "java";
  static final String ENCODING_NONE = "none";

  private static final byte[] SENTINEL_PREFIX =
      SENTINEL.substring(0, 8).getBytes(StandardCharsets.UTF_8);

  private TextGameDataFormat() {}

  /** True if {@code prefix} (the first decompressed bytes) begins the text-format sentinel. */
  static boolean startsWithSentinel(final byte[] prefix) {
    if (prefix.length < SENTINEL_PREFIX.length) {
      return false;
    }
    for (int i = 0; i < SENTINEL_PREFIX.length; i++) {
      if (prefix[i] != SENTINEL_PREFIX[i]) {
        return false;
      }
    }
    return true;
  }

  static JsonObject endMarker() {
    final JsonObject json = new JsonObject();
    json.addProperty("section", SECTION_END);
    return json;
  }
}
