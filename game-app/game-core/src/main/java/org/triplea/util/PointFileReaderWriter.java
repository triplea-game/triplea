package org.triplea.util;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.common.primitives.Ints;
import java.awt.Point;
import java.awt.Polygon;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility to read and write files in the form of String -> a list of points, or string-> list of
 * polygons.
 */
public final class PointFileReaderWriter {

  // Matches an int tuple like this: (123, 456)
  private static final Pattern pointPattern =
      Pattern.compile("\\s*\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)");
  // Matches a "polygon" like this: < something that's not a greater than or less than char >
  private static final Pattern polygonPattern = Pattern.compile("<[^>]*>");
  // Matches a Name-Int-Tuple pair like this: Some Weird Territory Name without an opening round
  // bracket (654, 321)
  private static final Pattern singlePointPattern =
      Pattern.compile("^([^(]*?)\\s*\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)");

  private PointFileReaderWriter() {}

  /** Returns a map of the form String -> Point. */
  public static Map<String, Point> readOneToOne(final Path input) throws IOException {
    checkNotNull(input);

    final Map<String, Point> mapping = new LinkedHashMap<>();
    readPath(input, current -> readSingle(current, mapping));
    return mapping;
  }

  private static void readSingle(final String line, final Map<String, Point> mapping) {
    final Matcher matcher = singlePointPattern.matcher(line);
    if (matcher.find()) {
      final String territoryName = matcher.group(1);
      if (mapping.containsKey(territoryName)) {
        throw new IllegalArgumentException(
            "Territory '"
                + territoryName
                + "' was found twice, it's already mapped to '"
                + mapping.get(territoryName)
                + "'");
      }
      mapping.put(territoryName, readPoint(matcher.group(2), matcher.group(3)));
    } else {
      throw new IllegalArgumentException(
          "Line '" + line + "' did not match required pattern. Example: Territory (1,2)");
    }
  }

  /**
   * Writes the specified one-to-one mapping between names and points to the specified stream.
   *
   * @param output The stream to which the name-to-point mappings will be written.
   * @param mapping The name-to-point mapping to be written.
   * @throws IOException If an I/O error occurs while writing to the stream.
   */
  public static void writeOneToOne(final Path output, final Map<String, Point> mapping)
      throws IOException {
    checkNotNull(output);
    checkNotNull(mapping);

    write(
        mapping.entrySet().stream()
            .map(entry -> entry.getKey() + " " + pointToString(entry.getValue()))
            .collect(Collectors.joining("\r\n")),
        output);
  }

  /**
   * Writes the specified one-to-many mapping between names and polygons to the specified stream.
   *
   * @param output The stream to which the name-to-polygons mappings will be written.
   * @param mapping The name-to-polygons mapping to be written.
   * @throws IOException If an I/O error occurs while writing to the stream.
   */
  public static void writeOneToManyPolygons(
      final Path output, final Map<String, List<Polygon>> mapping) throws IOException {
    checkNotNull(output);
    checkNotNull(mapping);

    write(
        mapping.entrySet().stream()
            .map(
                entry ->
                    entry.getKey()
                        + " "
                        + entry.getValue().stream()
                            .map(PointFileReaderWriter::polygonToString)
                            .collect(Collectors.joining()))
            .collect(Collectors.joining("\r\n")),
        output);
  }

  private static void write(final String string, final Path output) throws IOException {
    Files.writeString(output, string);
  }

  private static String pointToString(final Point point) {
    return " (" + point.x + "," + point.y + ") ";
  }

  private static String polygonToString(final Polygon polygon) {
    return Streams.zip(
            Ints.asList(polygon.xpoints).stream(),
            Ints.asList(polygon.ypoints).stream(),
            Point::new)
        .map(PointFileReaderWriter::pointToString)
        .collect(Collectors.joining("", " < ", " > "));
  }

  /**
   * Writes the specified one-to-many mapping between names and points to the specified stream.
   *
   * @param output The stream to which the name-to-points mappings will be written.
   * @param mapping The name-to-points mapping to be written.
   * @throws IOException If an I/O error occurs while writing to the stream.
   */
  public static void writeOneToMany(final Path output, final Map<String, List<Point>> mapping)
      throws IOException {
    checkNotNull(output);
    checkNotNull(mapping);

    write(
        mapping.entrySet().stream()
            .map(entry -> entry.getKey() + " " + pointsToString(entry.getValue()))
            .collect(Collectors.joining("\r\n")),
        output);
  }

  private static String pointsToString(final List<Point> points) {
    return points.stream().map(PointFileReaderWriter::pointToString).collect(Collectors.joining());
  }

  /** Returns a map of the form String -> Collection of points. */
  public static Map<String, List<Point>> readOneToMany(final Path input) throws IOException {
    checkNotNull(input);

    final Map<String, List<Point>> mapping = new LinkedHashMap<>();
    readPath(input, current -> readMultiple(current, mapping));
    return mapping;
  }

  /**
   * Writes the specified one-to-many mapping between names and (points, overflowToLeft) to the
   * specified stream.
   *
   * @param output The stream to which the name-to-points mappings will be written.
   * @param mapping The name-to-points mapping to be written.
   * @throws IOException If an I/O error occurs while writing to the stream.
   */
  public static void writeOneToManyPlacements(
      final Path output, final Map<String, Tuple<List<Point>, Boolean>> mapping)
      throws IOException {
    checkNotNull(output);
    checkNotNull(mapping);

    write(
        mapping.entrySet().stream()
            .map(
                entry ->
                    entry.getKey()
                        + " "
                        + pointsToString(entry.getValue().getFirst())
                        + " | overflowToLeft="
                        + entry.getValue().getSecond())
            .collect(Collectors.joining("\r\n")),
        output);
  }

  /** Returns a map of the form String -> (points, overflowToLeft). */
  public static Map<String, Tuple<List<Point>, Boolean>> readOneToManyPlacements(final Path input)
      throws IOException {
    checkNotNull(input);

    final Map<String, List<Point>> mapping = new LinkedHashMap<>();
    final Map<String, Tuple<List<Point>, Boolean>> result = new LinkedHashMap<>();
    readPath(
        input,
        current -> {
          final List<String> s = Splitter.on(" | ").splitToList(current);
          final Tuple<String, List<Point>> tuple = readMultiple(s.get(0), mapping);
          final boolean overflowToLeft =
              (s.size() == 2)
                  && Boolean.parseBoolean(Iterables.get(Splitter.on('=').split(s.get(1)), 1));
          result.put(tuple.getFirst(), Tuple.of(tuple.getSecond(), overflowToLeft));
        });
    return result;
  }

  /** Returns a map of the form String -> Collection of polygons. */
  public static Map<String, List<Polygon>> readOneToManyPolygons(final Path input)
      throws IOException {
    checkNotNull(input);

    final Map<String, List<Polygon>> mapping = new LinkedHashMap<>();
    readPath(input, current -> readMultiplePolygons(current, mapping));
    return mapping;
  }

  private static void readMultiplePolygons(
      final String line, final Map<String, List<Polygon>> mapping) {
    int openingSignIndex = line.indexOf('<');
    if (openingSignIndex == -1) {
      throw new IllegalArgumentException(
          MessageFormat.format("line misses opening sign ''<'': {0}", line));
    }
    final String name = line.substring(0, openingSignIndex).trim();
    if (mapping.containsKey(name)) {
      throw new IllegalArgumentException(MessageFormat.format("name found twice: {0}", name));
    }
    final List<Polygon> polygons = new ArrayList<>();
    final Matcher polyMatcher = polygonPattern.matcher(line);
    while (polyMatcher.find()) {
      final List<Point> points = new ArrayList<>();
      final Matcher pointMatcher = pointPattern.matcher(polyMatcher.group());
      while (pointMatcher.find()) {
        points.add(readPoint(pointMatcher.group(1), pointMatcher.group(2)));
      }
      polygons.add(newPolygonFromPoints(points));
    }
    mapping.put(name, polygons);
  }

  private static Polygon newPolygonFromPoints(final List<Point> points) {
    return new Polygon(
        points.stream().mapToInt(it -> it.x).toArray(),
        points.stream().mapToInt(it -> it.y).toArray(),
        points.size());
  }

  private static Tuple<String, List<Point>> readMultiple(
      final String line, final Map<String, List<Point>> mapping) {
    final String name = line.substring(0, line.indexOf("(")).trim();
    if (mapping.containsKey(name)) {
      throw new IllegalArgumentException("name found twice: " + name);
    }
    final Matcher matcher = pointPattern.matcher(line);
    final List<Point> points = new ArrayList<>();
    while (matcher.find()) {
      points.add(readPoint(matcher.group(1), matcher.group(2)));
    }
    mapping.put(name, points);
    return Tuple.of(name, points);
  }

  private static Point readPoint(String xStr, String yStr) {
    return new Point(Integer.parseInt(xStr), Integer.parseInt(yStr));
  }

  @VisibleForTesting
  static void readPath(final Path input, final Consumer<String> lineParser) throws IOException {

    try {
      Files.lines(input).filter(Predicate.not(String::isBlank)).forEachOrdered(lineParser);
    } catch (final IllegalArgumentException e) {
      throw new IOException(e);
    }
  }
}
