package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Point;
import java.awt.Polygon;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Nullable;

import org.apache.commons.io.input.CloseShieldInputStream;

/**
 * Utility to read and write files in the form of
 * String -> a list of points, or string-> list of polygons.
 */
public final class PointFileReaderWriter {
  private PointFileReaderWriter() {}

  /**
   * Returns a map of the form String -> Point.
   */
  public static Map<String, Point> readOneToOne(final InputStream stream) throws IOException {
    checkNotNull(stream);

    final Map<String, Point> mapping = new HashMap<>();
    try (InputStreamReader inputStreamReader = new InputStreamReader(new CloseShieldInputStream(stream));
        LineNumberReader reader = new LineNumberReader(inputStreamReader)) {
      @Nullable
      String current = reader.readLine();
      while (current != null) {
        if (current.trim().length() != 0) {
          readSingle(current, mapping);
        }
        current = reader.readLine();
      }
    }
    return mapping;
  }

  private static void readSingle(final String line, final Map<String, Point> mapping) throws IOException {
    final StringTokenizer tokens = new StringTokenizer(line, "", false);
    final String name = tokens.nextToken("(").trim();
    if (mapping.containsKey(name)) {
      throw new IOException("name found twice:" + name);
    }
    final int x = Integer.parseInt(tokens.nextToken("(, "));
    final int y = Integer.parseInt(tokens.nextToken(",) "));
    final Point p = new Point(x, y);
    mapping.put(name, p);
  }

  public static void writeOneToOne(final OutputStream sink, final Map<String, Point> mapping) throws Exception {
    final StringBuilder out = new StringBuilder();
    final Iterator<String> keyIter = mapping.keySet().iterator();
    while (keyIter.hasNext()) {
      final String name = keyIter.next();
      out.append(name).append(" ");
      final Point point = mapping.get(name);
      out.append(" (").append(point.x).append(",").append(point.y).append(")");
      if (keyIter.hasNext()) {
        out.append("\r\n");
      }
    }
    write(out, sink);
  }

  public static void writeOneToManyPolygons(final OutputStream sink, final Map<String, List<Polygon>> mapping)
      throws Exception {
    final StringBuilder out = new StringBuilder();
    final Iterator<String> keyIter = mapping.keySet().iterator();
    while (keyIter.hasNext()) {
      final String name = keyIter.next();
      out.append(name).append(" ");
      final List<Polygon> points = mapping.get(name);
      final Iterator<Polygon> polygonIter = points.iterator();
      while (polygonIter.hasNext()) {
        out.append(" < ");
        final Polygon polygon = polygonIter.next();
        for (int i = 0; i < polygon.npoints; i++) {
          out.append(" (").append(polygon.xpoints[i]).append(",").append(polygon.ypoints[i]).append(")");
        }
        out.append(" > ");
      }
      if (keyIter.hasNext()) {
        out.append("\r\n");
      }
    }
    write(out, sink);
  }

  private static void write(final StringBuilder buf, final OutputStream sink) throws IOException {
    final OutputStreamWriter out = new OutputStreamWriter(sink);
    out.write(buf.toString());
    out.flush();
  }

  public static void writeOneToMany(final OutputStream sink, Map<String, Collection<Point>> mapping) throws Exception {
    final StringBuilder out = new StringBuilder();
    if (mapping == null) {
      mapping = new HashMap<>();
    }
    final Iterator<String> keyIter = mapping.keySet().iterator();
    while (keyIter.hasNext()) {
      final String name = keyIter.next();
      out.append(name).append(" ");
      final Collection<Point> points = mapping.get(name);
      final Iterator<Point> pointIter = points.iterator();
      while (pointIter.hasNext()) {
        final Point point = pointIter.next();
        out.append(" (").append(point.x).append(",").append(point.y).append(")");
        if (pointIter.hasNext()) {
          out.append(" ");
        }
      }
      if (keyIter.hasNext()) {
        out.append("\r\n");
      }
    }
    write(out, sink);
  }

  /**
   * Returns a map of the form String -> Collection of points.
   */
  public static Map<String, List<Point>> readOneToMany(final InputStream stream) throws IOException {
    checkNotNull(stream);

    final HashMap<String, List<Point>> mapping = new HashMap<>();
    try (InputStreamReader inputStreamReader = new InputStreamReader(new CloseShieldInputStream(stream));
        LineNumberReader reader = new LineNumberReader(inputStreamReader)) {
      @Nullable
      String current = reader.readLine();
      while (current != null) {
        if (current.trim().length() != 0) {
          readMultiple(current, mapping);
        }
        current = reader.readLine();
      }
    }
    return mapping;
  }

  /**
   * Returns a map of the form String -> Collection of polygons.
   */
  public static Map<String, List<Polygon>> readOneToManyPolygons(final InputStream stream) throws IOException {
    final HashMap<String, List<Polygon>> mapping = new HashMap<>();
    try (InputStreamReader inputStreamReader = new InputStreamReader(new CloseShieldInputStream(stream));
        LineNumberReader reader = new LineNumberReader(inputStreamReader)) {
      @Nullable
      String current = reader.readLine();
      while (current != null) {
        if (current.trim().length() != 0) {
          readMultiplePolygons(current, mapping);
        }
        current = reader.readLine();
      }
    }
    return mapping;
  }

  private static void readMultiplePolygons(final String line, final HashMap<String, List<Polygon>> mapping)
      throws IOException {
    try {
      // this loop is executed a lot when loading games
      // so it is hand optimized
      final String name = line.substring(0, line.indexOf('<')).trim();
      int index = name.length();
      final List<Polygon> polygons = new ArrayList<>(64);
      final ArrayList<Point> points = new ArrayList<>();
      final int length = line.length();
      while (index < length) {
        char current = line.charAt(index);
        if (current == '<') {
          int x = 0;
          int y;
          int base = 0;
          // inside a poly
          while (true) {
            current = line.charAt(++index);
            switch (current) {
              case '0':
              case '1':
              case '2':
              case '3':
              case '4':
              case '5':
              case '6':
              case '7':
              case '8':
              case '9':
                base *= 10;
                base += current - '0';
                break;
              case ',':
                x = base;
                base = 0;
                break;
              case ')':
                y = base;
                base = 0;
                points.add(new Point(x, y));
                break;
              default:
                break;
            }
            if (current == '>') {
              // end poly
              createPolygonFromPoints(polygons, points);
              points.clear();
              // break from while(true)
              break;
            }
          }
        }
        index++;
      }
      if (mapping.containsKey(name)) {
        throw new IOException("name found twice:" + name);
      }
      mapping.put(name, polygons);
    } catch (final StringIndexOutOfBoundsException e) {
      throw new IllegalStateException("Invalid line:" + line, e);
    }
  }

  private static void createPolygonFromPoints(final Collection<Polygon> polygons, final ArrayList<Point> points) {
    final int[] pointsX = new int[points.size()];
    final int[] pointsY = new int[points.size()];
    for (int i = 0; i < points.size(); i++) {
      final Point p = points.get(i);
      pointsX[i] = p.x;
      pointsY[i] = p.y;
    }
    polygons.add(new Polygon(pointsX, pointsY, pointsX.length));
  }

  private static void readMultiple(final String line, final HashMap<String, List<Point>> mapping) throws IOException {
    final StringTokenizer tokens = new StringTokenizer(line, "");
    final String name = tokens.nextToken("(").trim();
    if (mapping.containsKey(name)) {
      throw new IOException("name found twice:" + name);
    }
    final List<Point> points = new ArrayList<>();
    while (tokens.hasMoreTokens()) {
      final String stringX = tokens.nextToken(",(), ");
      if (!tokens.hasMoreTokens()) {
        continue;
      }
      final String stringY = tokens.nextToken(",() ");
      final int x = Integer.parseInt(stringX);
      final int y = Integer.parseInt(stringY);
      points.add(new Point(x, y));
    }
    mapping.put(name, points);
  }
}
