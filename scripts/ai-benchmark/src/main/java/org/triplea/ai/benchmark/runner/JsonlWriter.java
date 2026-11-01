package org.triplea.ai.benchmark.runner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Appends one JSON object per line, flushed per record so a killed run keeps what it wrote. */
final class JsonlWriter implements Closeable {
  static final Gson GSON = new GsonBuilder().serializeNulls().create();

  private final BufferedWriter out;

  JsonlWriter(final Path file) throws IOException {
    out = Files.newBufferedWriter(file);
  }

  void write(final Map<String, Object> record) {
    try {
      out.write(GSON.toJson(record));
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
