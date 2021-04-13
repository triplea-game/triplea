package org.triplea.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.common.primitives.Ints;
import java.awt.Point;
import java.awt.Polygon;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.java.function.ThrowingConsumer;

final class PointFileReaderWriterTest {
  private static String writeToString(final ThrowingConsumer<Path, IOException> consumer)
      throws Exception {
    final Path path = mock(Path.class);
    final FileSystem fileSystem = mock(FileSystem.class);
    final FileSystemProvider fileSystemProvider = mock(FileSystemProvider.class);
    when(path.getFileSystem()).thenReturn(fileSystem);
    when(fileSystem.provider()).thenReturn(fileSystemProvider);
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    when(fileSystemProvider.newOutputStream(path)).thenReturn(os);
    consumer.accept(path);
    return os.toString(StandardCharsets.UTF_8);
  }

  private static Path pathToVirtualTextFile(final String content) throws IOException {
    final Path path = mock(Path.class);
    final FileSystem fileSystem = mock(FileSystem.class);
    final FileSystemProvider fileSystemProvider = mock(FileSystemProvider.class);
    when(path.getFileSystem()).thenReturn(fileSystem);
    when(fileSystem.provider()).thenReturn(fileSystemProvider);
    when(fileSystemProvider.newInputStream(path))
        .thenReturn(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    return path;
  }

  @Nested
  final class ReadOneToOneTest {
    @Test
    void shouldReturnEmptyMapWhenStreamIsEmpty() throws Exception {
      final Path path = pathToVirtualTextFile("");
      assertThat(PointFileReaderWriter.readOneToOne(path), is(Map.of()));
    }

    @Test
    void shouldReadOnePointPerName() throws Exception {
      final String content =
          ""
              // Questionable behaviour
              + "United Kingdom (1011,1021) (1234, 12424) everything here should be ignored\n"
              + "Germany (2011,2021)\n"
              + "Eastern United States (3011,3021)\n"
              + " (321,456)\n";

      final Map<String, Point> pointsByName =
          PointFileReaderWriter.readOneToOne(pathToVirtualTextFile(content));

      assertThat(
          pointsByName,
          is(
              Map.of(
                  "United Kingdom", new Point(1011, 1021),
                  "Germany", new Point(2011, 2021),
                  "Eastern United States", new Point(3011, 3021),
                  "", new Point(321, 456))));
    }

    @Test
    void shouldErrorOnInvalidSyntax() {
      final String content1 = "United Kingdom (1011,1021\n";
      assertThrows(
          IOException.class,
          () -> PointFileReaderWriter.readOneToOne(pathToVirtualTextFile(content1)));

      final String content2 = "(United) Kingdom (1011,1021)\n";
      assertThrows(
          IOException.class,
          () -> PointFileReaderWriter.readOneToOne(pathToVirtualTextFile(content2)));

      final String content3 = "United Kingdom 1011,1021)\n";
      assertThrows(
          IOException.class,
          () -> PointFileReaderWriter.readOneToOne(pathToVirtualTextFile(content3)));

      final String content4 = "United Kingdom (1011 1021)\n";
      assertThrows(
          IOException.class,
          () -> PointFileReaderWriter.readOneToOne(pathToVirtualTextFile(content4)));

      final String content5 = "United Kingdom 1011 1021\n";
      assertThrows(
          IOException.class,
          () -> PointFileReaderWriter.readOneToOne(pathToVirtualTextFile(content5)));
    }

    @Test
    void shouldErrorOnDuplicateKey() {
      final String content1 = "" + "54 Sea Zone  (1011,1021)\n" + "54 Sea Zone  (1011,1021)";

      final Exception e1 =
          assertThrows(
              IOException.class,
              () -> PointFileReaderWriter.readOneToOne(pathToVirtualTextFile(content1)));
      assertTrue(e1.getMessage().contains("54 Sea Zone"));

      final String content2 =
          "" + "54 Sea Zone  (1011,1021)\n" + "54 Sea Zone  (      1011  , 1021    )   ";

      final Exception e2 =
          assertThrows(
              IOException.class,
              () -> PointFileReaderWriter.readOneToOne(pathToVirtualTextFile(content2)));
      assertTrue(e2.getMessage().contains("54 Sea Zone"));

      final String content3 = "" + "54 Sea Zone  (1011,1021)\n" + "54 Sea Zone  (1021,1011)";

      final Exception e3 =
          assertThrows(
              IOException.class,
              () -> PointFileReaderWriter.readOneToOne(pathToVirtualTextFile(content3)));
      assertTrue(e3.getMessage().contains("54 Sea Zone"));
    }

    @Test
    void shouldSupportNegativeValues() throws Exception {
      final String content =
          ""
              + "United Kingdom (-1011,1021)\n"
              + "Germany (1234, -12424)\n"
              + "Eastern United States (-123, -456)";

      final Map<String, Point> pointsByName =
          PointFileReaderWriter.readOneToOne(pathToVirtualTextFile(content));

      assertThat(
          pointsByName,
          is(
              Map.of(
                  "United Kingdom", new Point(-1011, 1021),
                  "Germany", new Point(1234, -12424),
                  "Eastern United States", new Point(-123, -456))));
    }
  }

  @Nested
  final class ReadOneToManyTest {
    @Test
    void shouldReturnEmptyMapWhenStreamIsEmpty() throws Exception {
      assertThat(PointFileReaderWriter.readOneToMany(pathToVirtualTextFile("")), is(Map.of()));
    }

    @Test
    void shouldReadMultiplePointsPerName() throws Exception {
      final String content =
          ""
              + "Belarus  (1011,1021)  (1012,1022)  (1013,1023)\n"
              + "54 Sea Zone  (2011,2021)  (2012,2022)\n"
              + "Philippines (3011,3021)\n";

      final Map<String, List<Point>> pointListsByName =
          PointFileReaderWriter.readOneToMany(pathToVirtualTextFile(content));

      assertThat(
          pointListsByName,
          is(
              Map.of(
                  "Belarus",
                      List.of(new Point(1011, 1021), new Point(1012, 1022), new Point(1013, 1023)),
                  "54 Sea Zone", List.of(new Point(2011, 2021), new Point(2012, 2022)),
                  "Philippines", List.of(new Point(3011, 3021)))));
    }

    @Test
    void shouldErrorOnDuplicateKey() {
      final String content1 = "" + "54 Sea Zone  (1011,1021)\n" + "54 Sea Zone  (1011,1021)";

      final Exception e1 =
          assertThrows(
              IOException.class,
              () -> PointFileReaderWriter.readOneToMany(pathToVirtualTextFile(content1)));
      assertTrue(e1.getMessage().contains("54 Sea Zone"));

      final String content2 =
          "" + "54 Sea Zone  (1011,1021)\n" + "54 Sea Zone  (      1011  , 1021    )   ";

      final Exception e2 =
          assertThrows(
              IOException.class,
              () -> PointFileReaderWriter.readOneToMany(pathToVirtualTextFile(content2)));
      assertTrue(e2.getMessage().contains("54 Sea Zone"));

      final String content3 =
          ""
              + "54 Sea Zone  (1011,1021)  (1012,1022)  (1013,1023)\n"
              + "54 Sea Zone  (2011,2021)  (2012,2022)";

      final Exception e3 =
          assertThrows(
              IOException.class,
              () -> PointFileReaderWriter.readOneToMany(pathToVirtualTextFile(content3)));
      assertTrue(e3.getMessage().contains("54 Sea Zone"));
    }

    @Test
    void shouldSupportNegativeValues() throws Exception {
      final String content = "" + "United Kingdom (-1011,1021) (1234, -12424) (-123, -456)";

      final Map<String, List<Point>> pointListsByName =
          PointFileReaderWriter.readOneToMany(pathToVirtualTextFile(content));

      assertThat(
          pointListsByName,
          is(
              Map.of(
                  "United Kingdom",
                  List.of(
                      new Point(-1011, 1021), new Point(1234, -12424), new Point(-123, -456)))));
    }
  }

  @Nested
  final class ReadOneToManyPlacementsTest {
    @Test
    void shouldReturnEmptyMapWhenStreamIsEmpty() throws Exception {
      assertThat(
          PointFileReaderWriter.readOneToManyPlacements(pathToVirtualTextFile("")), is(Map.of()));
    }

    @Test
    void shouldReadMultiplePlacementsPerName() throws Exception {
      final String content =
          ""
              + "Belarus  (1011,1021)  (1012,1022)  (1013,1023)  | overflowToLeft=false\n"
              + "54 Sea Zone  (2011,2021)  (2012,2022)     | overflowToLeft=true\n"
              + "Philippines (3011,3021)     | weird other thing =true \n"
              + "East America (4011,4021)\n"
              + "East Africa (5011,5021) | overflowToLeft=not a boolean\n";

      final Map<String, Tuple<List<Point>, Boolean>> pointListsByName =
          PointFileReaderWriter.readOneToManyPlacements(pathToVirtualTextFile(content));

      assertThat(
          pointListsByName,
          is(
              Map.of(
                  "Belarus",
                  Tuple.of(
                      Arrays.asList(
                          new Point(1011, 1021), new Point(1012, 1022), new Point(1013, 1023)),
                      false),
                  "54 Sea Zone",
                  Tuple.of(List.of(new Point(2011, 2021), new Point(2012, 2022)), true),
                  "Philippines",
                  Tuple.of(List.of(new Point(3011, 3021)), false),
                  "East America",
                  Tuple.of(List.of(new Point(4011, 4021)), false),
                  "East Africa",
                  Tuple.of(List.of(new Point(5011, 5021)), false))));
    }

    @Test
    void shouldErrorOnDuplicateKey() {
      final String content1 =
          ""
              + "54 Sea Zone  (1011,1021)  | overflowToLeft=false\n"
              + "54 Sea Zone  (1011,1021)  | overflowToLeft=false";

      final Exception e1 =
          assertThrows(
              IOException.class,
              () -> PointFileReaderWriter.readOneToManyPlacements(pathToVirtualTextFile(content1)));
      assertTrue(e1.getMessage().contains("54 Sea Zone"));

      final String content2 =
          ""
              + "54 Sea Zone  (1011,1021)  | overflowToLeft=true\n"
              + "54 Sea Zone  (      1011  , 1021    )    | overflowToLeft=true";

      final Exception e2 =
          assertThrows(
              IOException.class,
              () -> PointFileReaderWriter.readOneToManyPlacements(pathToVirtualTextFile(content2)));
      assertTrue(e2.getMessage().contains("54 Sea Zone"));

      final String content3 =
          ""
              + "54 Sea Zone  (1011,1021)  (1012,1022)  (1013,1023)  | qwertz =false\n"
              + "54 Sea Zone  (2011,2021)  (2012,2022)  | overflowToLeft=true";

      final Exception e3 =
          assertThrows(
              IOException.class,
              () -> PointFileReaderWriter.readOneToManyPlacements(pathToVirtualTextFile(content3)));
      assertTrue(e3.getMessage().contains("54 Sea Zone"));
    }

    @Test
    void shouldSupportNegativeValues() throws Exception {
      final String content = "" + "United Kingdom (-1011,1021) (1234, -12424) (-123, -456)";

      final Map<String, Tuple<List<Point>, Boolean>> pointListsByName =
          PointFileReaderWriter.readOneToManyPlacements(pathToVirtualTextFile(content));

      assertThat(
          pointListsByName,
          is(
              Map.of(
                  "United Kingdom",
                  Tuple.of(
                      List.of(
                          new Point(-1011, 1021), new Point(1234, -12424), new Point(-123, -456)),
                      Boolean.FALSE))));
    }
  }

  @Nested
  final class ReadOneToManyPolygonsTest {
    @Test
    void shouldReturnEmptyMapWhenStreamIsEmpty() throws Exception {
      assertThat(
          PointFileReaderWriter.readOneToManyPolygons(pathToVirtualTextFile("")), is(Map.of()));
    }

    @Test
    void shouldReadMultiplePolygonsPerName() throws Exception {
      final String content =
          "Belarus  <  (1011,1021) (1012,1022) (1013,1023) >\n"
              + "54 Sea Zone  <  (2011,2021) (2012,2022) (2013,2023) >  <  "
              + "(2111,2121) (2112,2122) (2113,2123) >\n"
              + "Philippines  <  (3011,3021) (3012,3022) (3013,3023) >  <  "
              + "(3111,3121) (3112,3122) >  <  (3211,3221) >\n";

      final Map<String, List<Polygon>> polygonListsByName =
          PointFileReaderWriter.readOneToManyPolygons(pathToVirtualTextFile(content));

      assertThat(polygonListsByName, is(aMapWithSize(3)));
      assertThat(polygonListsByName, hasKey("Belarus"));
      assertThat(
          points(polygonListsByName.get("Belarus")),
          is(
              List.of(
                  List.of(new Point(1011, 1021), new Point(1012, 1022), new Point(1013, 1023)))));
      assertThat(polygonListsByName, hasKey("54 Sea Zone"));
      assertThat(
          points(polygonListsByName.get("54 Sea Zone")),
          is(
              List.of(
                  List.of(new Point(2011, 2021), new Point(2012, 2022), new Point(2013, 2023)),
                  List.of(new Point(2111, 2121), new Point(2112, 2122), new Point(2113, 2123)))));
      assertThat(polygonListsByName, hasKey("Philippines"));
      assertThat(
          points(polygonListsByName.get("Philippines")),
          is(
              List.of(
                  List.of(new Point(3011, 3021), new Point(3012, 3022), new Point(3013, 3023)),
                  List.of(new Point(3111, 3121), new Point(3112, 3122)),
                  List.of(new Point(3211, 3221)))));
    }

    @Test
    void shouldErrorOnDuplicateKey() {
      final String content1 =
          ""
              + "54 Sea Zone  <  (1011,1021) (1012,1022) (1013,1023) >\n"
              + "54 Sea Zone  <  (1011,1021) (1012,1022) (1013,1023) >";

      final Exception e1 =
          assertThrows(
              IOException.class,
              () -> PointFileReaderWriter.readOneToManyPolygons(pathToVirtualTextFile(content1)));
      assertTrue(e1.getMessage().contains("54 Sea Zone"));

      final String content2 =
          "54 Sea Zone  <  (1011,1021) (1012,1022) (1013,1023) >\n"
              + "54 Sea Zone  <  (1011,1021) (1012,1022) (1013,1023) >  <  "
              + "(2111,2121) (2112,2122) (2113,2123) >";
      final Exception e2 =
          assertThrows(
              IOException.class,
              () -> PointFileReaderWriter.readOneToManyPolygons(pathToVirtualTextFile(content2)));
      assertTrue(e2.getMessage().contains("54 Sea Zone"));

      final String content3 =
          "54 Sea Zone  <  (1011,1021) (1012,1022) (1013,1023) >\n"
              + "54 Sea Zone  <  (2011 , 2021) (2012 , 2022 ) ( 2013,21023) >  <  "
              + "(2111,2121) (2112,2122) (2113,2123) >";
      final Exception e3 =
          assertThrows(
              IOException.class,
              () -> PointFileReaderWriter.readOneToManyPolygons(pathToVirtualTextFile(content3)));
      assertTrue(e3.getMessage().contains("54 Sea Zone"));
    }

    private List<List<Point>> points(final List<Polygon> polygons) {
      return polygons.stream()
          .map(
              polygon ->
                  Streams.zip(
                          Ints.asList(polygon.xpoints).stream(),
                          Ints.asList(polygon.ypoints).stream(),
                          Point::new)
                      .collect(Collectors.toList()))
          .collect(Collectors.toList());
    }

    @Test
    void shouldSupportNegativeValues() throws Exception {
      final String content = "" + "United Kingdom < (-1011,1021) (1234, -12424) (-123, -456) >";

      final Map<String, List<Polygon>> polygonListsByName =
          PointFileReaderWriter.readOneToManyPolygons(pathToVirtualTextFile(content));

      assertThat(
          points(polygonListsByName.get("United Kingdom")),
          is(
              List.of(
                  List.of(
                      new Point(-1011, 1021), new Point(1234, -12424), new Point(-123, -456)))));
    }
  }

  @Nested
  final class WriteOneToOneTest {
    @Test
    void shouldWriteOnePointPerName() throws Exception {
      final Map<String, Point> pointsByName =
          // ImmutableMap#of guarantees iteration ordering
          ImmutableMap.of(
              "United Kingdom", new Point(1011, 1021),
              "Germany", new Point(2011, 2021),
              "Eastern United States", new Point(3011, 3021));

      final String content =
          writeToString(os -> PointFileReaderWriter.writeOneToOne(os, pointsByName));

      assertThat(
          content,
          is(
              ""
                  + "United Kingdom  (1011,1021) \r\n"
                  + "Germany  (2011,2021) \r\n"
                  + "Eastern United States  (3011,3021) "));
    }
  }

  @Nested
  final class WriteOneToManyTest {
    @Test
    void shouldWriteMultiplePointsPerName() throws Exception {
      final Map<String, List<Point>> pointListsByName =
          // ImmutableMap#of guarantees iteration ordering
          ImmutableMap.of(
              "Belarus",
                  List.of(new Point(1011, 1021), new Point(1012, 1022), new Point(1013, 1023)),
              "54 Sea Zone", List.of(new Point(2011, 2021), new Point(2012, 2022)),
              "Philippines", List.of(new Point(3011, 3021)));

      final String content =
          writeToString(os -> PointFileReaderWriter.writeOneToMany(os, pointListsByName));

      assertThat(
          content,
          is(
              ""
                  + "Belarus  (1011,1021)  (1012,1022)  (1013,1023) \r\n"
                  + "54 Sea Zone  (2011,2021)  (2012,2022) \r\n"
                  + "Philippines  (3011,3021) "));
    }
  }

  @Nested
  final class WriteOneToManyPlacementsTest {
    @Test
    void shouldWriteMultiplePlacementsPerName() throws Exception {
      final Map<String, Tuple<List<Point>, Boolean>> polygonListsByName =
          // ImmutableMap#of guarantees iteration ordering
          ImmutableMap.of(
              "Belarus",
                  Tuple.of(
                      List.of(new Point(1011, 1021), new Point(1012, 1022), new Point(1013, 1023)),
                      true),
              "54 Sea Zone",
                  Tuple.of(
                      List.of(new Point(2011, 2021), new Point(2012, 2022), new Point(2013, 2023)),
                      false),
              "Philippines",
                  Tuple.of(
                      List.of(new Point(3011, 3021), new Point(3012, 3022), new Point(3013, 3023)),
                      true));

      final String content =
          writeToString(
              os -> PointFileReaderWriter.writeOneToManyPlacements(os, polygonListsByName));

      assertThat(
          content,
          is(
              ""
                  + "Belarus  (1011,1021)  (1012,1022)  (1013,1023)  | overflowToLeft=true\r\n"
                  + "54 Sea Zone  (2011,2021)  (2012,2022)  (2013,2023)  | overflowToLeft=false\r\n"
                  + "Philippines  (3011,3021)  (3012,3022)  (3013,3023)  | overflowToLeft=true"));
    }
  }

  @Nested
  final class WriteOneToManyPolygonsTest {
    @Test
    void shouldWriteMultiplePolygonsPerName() throws Exception {
      final Map<String, List<Polygon>> polygonListsByName =
          // ImmutableMap#of guarantees iteration ordering
          ImmutableMap.of(
              "Belarus",
              List.of(polygon(new Point(1011, 1021), new Point(1012, 1022), new Point(1013, 1023))),
              "54 Sea Zone",
              List.of(
                  polygon(new Point(2011, 2021), new Point(2012, 2022), new Point(2013, 2023)),
                  polygon(new Point(2111, 2121), new Point(2112, 2122), new Point(2113, 2123))),
              "Philippines",
              List.of(
                  polygon(new Point(3011, 3021), new Point(3012, 3022), new Point(3013, 3023)),
                  polygon(new Point(3111, 3121), new Point(3112, 3122)),
                  polygon(new Point(3211, 3221))));

      final String content =
          writeToString(os -> PointFileReaderWriter.writeOneToManyPolygons(os, polygonListsByName));

      assertThat(
          content,
          is(
              ""
                  + "Belarus  <  (1011,1021)  (1012,1022)  (1013,1023)  > \r\n"
                  + "54 Sea Zone  <  (2011,2021)  (2012,2022)  (2013,2023)  > "
                  + " <  (2111,2121)  (2112,2122)  (2113,2123)  > \r\n"
                  + "Philippines  <  (3011,3021)  (3012,3022)  (3013,3023)  > "
                  + " <  (3111,3121)  (3112,3122)  >  <  (3211,3221)  > "));
    }

    private Polygon polygon(final Point... points) {
      return new Polygon(
          Arrays.stream(points).mapToInt(it -> it.x).toArray(),
          Arrays.stream(points).mapToInt(it -> it.y).toArray(),
          points.length);
    }
  }

  @Nested
  final class ReadStreamTest {
    @Test
    void testExceptionWrapping() throws IOException {
      final String test = "Test";
      final Path path = pathToVirtualTextFile(test);
      final IllegalArgumentException exception = new IllegalArgumentException("Test Exception");
      final Exception e =
          assertThrows(
              IOException.class,
              () ->
                  PointFileReaderWriter.readPath(
                      path,
                      line -> {
                        assertEquals(test, line);
                        throw exception;
                      }));
      assertEquals(exception, e.getCause());
    }
  }
}
