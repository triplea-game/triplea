package org.triplea.ai.benchmark.runner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Finds kata: every direct subfolder of the kata folder holding a {@code kata.yaml}. A folder name
 * starting with "_" marks an internal kata (eg: the pipeline check) that a glob only matches when
 * it starts with "_" too, so {@code *} runs real kata only.
 */
final class KataCatalog {
  private final Path root;
  private final KataParser parser;

  KataCatalog(final Path root, final KataParser parser) {
    this.root = root;
    this.parser = parser;
  }

  List<Kata> all() {
    return folders().map(parser::parse).toList();
  }

  List<Kata> matching(final String glob) {
    final var matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
    return folders()
        .filter(dir -> matcher.matches(dir.getFileName()))
        .filter(dir -> glob.startsWith("_") || !dir.getFileName().toString().startsWith("_"))
        .map(parser::parse)
        .toList();
  }

  private Stream<Path> folders() {
    try (Stream<Path> entries = Files.list(root)) {
      return entries
          .filter(dir -> Files.isRegularFile(dir.resolve(KataParser.DESCRIPTOR)))
          .sorted()
          .toList()
          .stream();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
