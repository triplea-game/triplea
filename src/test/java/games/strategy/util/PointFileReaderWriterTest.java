package games.strategy.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.awt.Point;
import java.awt.Polygon;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.common.primitives.Ints;

import games.strategy.io.IoUtils;

public final class PointFileReaderWriterTest {
  @Nested
  public final class ReadOneToOneTest {
    @Test
    public void shouldNotCloseStream() throws Exception {
      final InputStream is = spy(new ByteArrayInputStream(new byte[0]));

      PointFileReaderWriter.readOneToOne(is);

      verify(is, never()).close();
    }

    @Test
    public void shouldReturnEmptyMapWhenStreamIsEmpty() throws Exception {
      assertThat(IoUtils.readFromMemory(new byte[0], PointFileReaderWriter::readOneToOne), is(Collections.emptyMap()));
    }

    @Test
    public void shouldReadOnePointPerName() throws Exception {
      final String content = ""
          + "United Kingdom (1011,1021)\n"
          + "Germany (2011,2021)\n"
          + "Eastern United States (3011,3021)\n";

      final Map<String, Point> pointsByName =
          IoUtils.readFromMemory(content.getBytes(StandardCharsets.UTF_8), PointFileReaderWriter::readOneToOne);

      assertThat(pointsByName, is(ImmutableMap.of(
          "United Kingdom", new Point(1011, 1021),
          "Germany", new Point(2011, 2021),
          "Eastern United States", new Point(3011, 3021))));
    }
  }

  @Nested
  public final class ReadOneToManyTest {
    @Test
    public void shouldNotCloseStream() throws Exception {
      final InputStream is = spy(new ByteArrayInputStream(new byte[0]));

      PointFileReaderWriter.readOneToMany(is);

      verify(is, never()).close();
    }

    @Test
    public void shouldReturnEmptyMapWhenStreamIsEmpty() throws Exception {
      assertThat(IoUtils.readFromMemory(new byte[0], PointFileReaderWriter::readOneToMany), is(Collections.emptyMap()));
    }

    @Test
    public void shouldReadMultiplePointsPerName() throws Exception {
      final String content = ""
          + "Belarus  (1011,1021)  (1012,1022)  (1013,1023)\n"
          + "54 Sea Zone  (2011,2021)  (2012,2022)\n"
          + "Philippines (3011,3021)\n";

      final Map<String, List<Point>> pointListsByName =
          IoUtils.readFromMemory(content.getBytes(StandardCharsets.UTF_8), PointFileReaderWriter::readOneToMany);

      assertThat(pointListsByName, is(ImmutableMap.of(
          "Belarus", Arrays.asList(new Point(1011, 1021), new Point(1012, 1022), new Point(1013, 1023)),
          "54 Sea Zone", Arrays.asList(new Point(2011, 2021), new Point(2012, 2022)),
          "Philippines", Arrays.asList(new Point(3011, 3021)))));
    }
  }

  @Nested
  public final class ReadOneToManyPolygonsTest {
    @Test
    public void shouldNotCloseStream() throws Exception {
      final InputStream is = spy(new ByteArrayInputStream(new byte[0]));

      PointFileReaderWriter.readOneToManyPolygons(is);

      verify(is, never()).close();
    }

    @Test
    public void shouldReturnEmptyMapWhenStreamIsEmpty() throws Exception {
      assertThat(
          IoUtils.readFromMemory(new byte[0], PointFileReaderWriter::readOneToManyPolygons),
          is(Collections.emptyMap()));
    }

    @Test
    public void shouldReadMultiplePolygonsPerName() throws Exception {
      final String content = ""
          + "Belarus  <  (1011,1021) (1012,1022) (1013,1023) >\n"
          + "54 Sea Zone  <  (2011,2021) (2012,2022) (2013,2023) >  <  (2111,2121) (2112,2122) (2113,2123) >\n"
          + "Philippines  <  (3011,3021) (3012,3022) (3013,3023) >  <  (3111,3121) (3112,3122) >  <  (3211,3221) >\n";

      final Map<String, List<Polygon>> polygonListsByName = IoUtils.readFromMemory(
          content.getBytes(StandardCharsets.UTF_8),
          PointFileReaderWriter::readOneToManyPolygons);

      assertThat(polygonListsByName, is(aMapWithSize(3)));
      assertThat(polygonListsByName, hasKey("Belarus"));
      assertThat(points(polygonListsByName.get("Belarus")), is(Arrays.asList(
          Arrays.asList(new Point(1011, 1021), new Point(1012, 1022), new Point(1013, 1023)))));
      assertThat(polygonListsByName, hasKey("54 Sea Zone"));
      assertThat(points(polygonListsByName.get("54 Sea Zone")), is(Arrays.asList(
          Arrays.asList(new Point(2011, 2021), new Point(2012, 2022), new Point(2013, 2023)),
          Arrays.asList(new Point(2111, 2121), new Point(2112, 2122), new Point(2113, 2123)))));
      assertThat(polygonListsByName, hasKey("Philippines"));
      assertThat(points(polygonListsByName.get("Philippines")), is(Arrays.asList(
          Arrays.asList(new Point(3011, 3021), new Point(3012, 3022), new Point(3013, 3023)),
          Arrays.asList(new Point(3111, 3121), new Point(3112, 3122)),
          Arrays.asList(new Point(3211, 3221)))));
    }

    private List<List<Point>> points(final List<Polygon> polygons) {
      return polygons.stream()
          .map(polygon -> {
            return Streams.zip(Ints.asList(polygon.xpoints).stream(), Ints.asList(polygon.ypoints).stream(), Point::new)
                .collect(Collectors.toList());
          })
          .collect(Collectors.toList());
    }
  }
}
