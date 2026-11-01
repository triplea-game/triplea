package org.triplea.ai.benchmark.runner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Appends one JSON object per line, flushed per record so a killed run keeps what it wrote. */
final class JsonlWriter implements Closeable {
  static final Gson GSON =
      new GsonBuilder()
          .serializeNulls()
          .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
          .create();

  static Map<String, Object> parse(final String json) {
    return GSON.fromJson(json, new TypeToken<LinkedHashMap<String, Object>>() {}.getType());
  }

  private final BufferedWriter out;

  JsonlWriter(final Path file) throws IOException {
    out = Files.newBufferedWriter(file);
  }

  void write(final Map<String, Object> record) {
    writeLine(GSON.toJson(record));
  }

  /** Appends a line that already holds one JSON object. */
  void writeLine(final String json) {
    try {
      out.write(json);
      out.newLine();
      out.flush();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    out.close();
  }
}
