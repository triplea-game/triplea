package games.strategy.engine.framework;

import com.google.gson.JsonObject;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverRegistry;
import games.strategy.engine.data.serializer.TextSaver;
import games.strategy.engine.delegate.IDelegate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.triplea.config.product.ProductVersionReader;

/**
 * Writes a game as the experimental text (JSONL) save format. See {@link TextGameDataFormat} for the
 * on-disk layout.
 *
 * <p>This is an incremental replacement for Java serialization. The monolithic {@code GameData}
 * object graph is emitted as one legacy-serialized {@code gameData} section (an entangled graph that
 * cannot yet be split), while each delegate's state is emitted natively as text when a {@link
 * TextSaver} is registered for its state class, and otherwise falls back to a legacy blob. As savers
 * are added, delegate sections migrate from {@code java} to {@code text} with no format change.
 */
final class TextGameDataWriter {

  private TextGameDataWriter() {}

  /** Writes {@code data} to {@code sink} (typically a GZIP stream) in the text save format. */
  static void save(final OutputStream sink, final GameData data) throws IOException {
    final GameRefResolver refs = new GameRefResolver(data);
    final Writer writer =
        new java.io.BufferedWriter(new java.io.OutputStreamWriter(sink, StandardCharsets.UTF_8));
    try (GameData.Unlocker ignored = data.acquireWriteLock()) {
      writeLine(writer, TextGameDataFormat.SENTINEL);
      writeJson(writer, header(data));
      writeJson(writer, gameDataSection(data));
      for (final IDelegate delegate : data.getDelegates()) {
        writeJson(writer, delegateSection(delegate, refs));
      }
      writeJson(writer, TextGameDataFormat.endMarker());
    }
    writer.flush();
  }

  private static JsonObject header(final GameData data) {
    final JsonObject json = new JsonObject();
    json.addProperty("section", TextGameDataFormat.SECTION_HEADER);
    json.addProperty("gameName", data.getGameName());
    json.addProperty("engineVersion", ProductVersionReader.getCurrentVersion().toString());
    return json;
  }

  /**
   * The whole {@code GameData} graph as a legacy-serialized blob. Attachment XML data is stripped
   * (as {@code Options.forSaveGame()} does); history is retained.
   */
  private static JsonObject gameDataSection(final GameData data) throws IOException {
    final var attachments = data.getAttachmentOrderAndValues();
    data.setAttachmentOrderAndValues(null);
    final byte[] bytes;
    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
        out.writeObject(ProductVersionReader.getCurrentVersion());
        out.writeObject(data);
      }
      bytes = bos.toByteArray();
    } finally {
      data.setAttachmentOrderAndValues(attachments);
    }
    final JsonObject json = new JsonObject();
    json.addProperty("section", TextGameDataFormat.SECTION_GAME_DATA);
    json.addProperty("encoding", TextGameDataFormat.ENCODING_JAVA);
    json.addProperty("bytes", Base64.getEncoder().encodeToString(bytes));
    return json;
  }

  private static JsonObject delegateSection(final IDelegate delegate, final GameRefResolver refs)
      throws IOException {
    final JsonObject json = new JsonObject();
    json.addProperty("section", TextGameDataFormat.SECTION_DELEGATE);
    json.addProperty("name", delegate.getName());
    json.addProperty("displayName", delegate.getDisplayName());
    json.addProperty("className", delegate.getClass().getName());

    final Serializable state = delegate.saveState();
    if (state == null) {
      json.addProperty("encoding", TextGameDataFormat.ENCODING_NONE);
      return json;
    }
    final Optional<TextSaver<Object>> saver = SaverRegistry.forInstance(state);
    if (saver.isPresent()) {
      json.addProperty("encoding", TextGameDataFormat.ENCODING_TEXT);
      json.addProperty("stateClass", state.getClass().getName());
      json.add("state", saver.get().write(state, refs));
    } else {
      json.addProperty("encoding", TextGameDataFormat.ENCODING_JAVA);
      json.addProperty("bytes", toJavaBlob(state));
    }
    return json;
  }

  private static String toJavaBlob(final Serializable value) throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(value);
    }
    return Base64.getEncoder().encodeToString(bos.toByteArray());
  }

  private static void writeJson(final Writer writer, final JsonObject json) throws IOException {
    writeLine(writer, TextGameDataFormat.GSON.toJson(json));
  }

  private static void writeLine(final Writer writer, final String line) throws IOException {
    writer.write(line);
    writer.write('\n');
  }
}
