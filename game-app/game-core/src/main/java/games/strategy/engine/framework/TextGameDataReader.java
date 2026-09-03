package games.strategy.engine.framework;

import com.google.gson.JsonObject;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.delegate.IDelegate;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Reads the experimental text (JSONL) save format written by {@link TextGameDataWriter}. The
 * counterpart of the legacy {@code GameDataManager.loadGameUncompressed}: it rebuilds the {@code
 * GameData} graph from the {@code gameData} blob, then reconstructs each delegate — loading its
 * state natively when written as text (via the registered {@link TextSaver}) and from a legacy blob
 * otherwise.
 */
final class TextGameDataReader {

  private TextGameDataReader() {}

  /**
   * Reads a text-format game from {@code source} (the decompressed stream, positioned at the
   * sentinel).
   */
  static GameData load(final InputStream source) throws IOException, ClassNotFoundException {
    final BufferedReader reader =
        new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8));

    final String sentinel = reader.readLine();
    if (!TextGameDataFormat.SENTINEL.equals(sentinel)) {
      throw new IOException("Not a text-format save; unexpected sentinel: " + sentinel);
    }

    GameData data = null;
    GameRefResolver refs = null;
    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
      if (line.isEmpty()) {
        continue;
      }
      final JsonObject record = TextGameDataFormat.GSON.fromJson(line, JsonObject.class);
      final String section = record.get("section").getAsString();
      switch (section) {
        case TextGameDataFormat.SECTION_HEADER -> {
          // header carries gameName/engineVersion; the gameData blob is authoritative for state.
        }
        case TextGameDataFormat.SECTION_GAME_DATA -> {
          data = readGameData(record);
          refs = new GameRefResolver(data);
        }
        case TextGameDataFormat.SECTION_DELEGATE -> {
          if (data == null) {
            throw new IOException("delegate section appeared before gameData section");
          }
          readDelegate(record, data, refs);
        }
        case TextGameDataFormat.SECTION_END -> {
          // explicit end; loop also terminates on EOF.
        }
        default -> throw new IOException("Unknown save section: " + section);
      }
    }
    if (data == null) {
      throw new IOException("Text save contained no gameData section");
    }
    data.fixUpNullPlayersInDelegates();
    return data;
  }

  private static GameData readGameData(final JsonObject record)
      throws IOException, ClassNotFoundException {
    final byte[] bytes = Base64.getDecoder().decode(record.get("bytes").getAsString());
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      in.readObject(); // version, unused
      final GameData data = (GameData) in.readObject();
      data.postDeSerialize();
      return data;
    }
  }

  private static void readDelegate(
      final JsonObject record, final GameData data, final GameRefResolver refs)
      throws IOException, ClassNotFoundException {
    final String name = record.get("name").getAsString();
    final String displayName = record.get("displayName").getAsString();
    final String className = record.get("className").getAsString();

    final IDelegate instance;
    try {
      instance =
          Class.forName(className)
              .asSubclass(IDelegate.class)
              .getDeclaredConstructor()
              .newInstance();
      instance.initialize(name, displayName);
      data.addDelegate(instance);
    } catch (final ReflectiveOperationException e) {
      throw new IOException("Failed to construct delegate " + className, e);
    }

    instance.loadState(SaverSupport.readNested(record.getAsJsonObject("state"), refs));
  }
}
