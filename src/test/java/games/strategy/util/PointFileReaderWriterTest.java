package games.strategy.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

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
          + "United Kingdom (1865,475)\n"
          + "Germany (2470,667)\n"
          + "Eastern United States (715,655)\n";

      final Map<String, Point> pointsByName =
          IoUtils.readFromMemory(content.getBytes(StandardCharsets.UTF_8), PointFileReaderWriter::readOneToOne);

      assertThat(pointsByName, is(ImmutableMap.of(
          "United Kingdom", new Point(1865, 475),
          "Germany", new Point(2470, 667),
          "Eastern United States", new Point(715, 655))));
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
    public void shouldReturnEmptyMapWhenStreamIsNull() {
      assertThat(PointFileReaderWriter.readOneToMany(null), is(Collections.emptyMap()));
    }

    @Test
    public void shouldReadMultiplePointsPerName() throws Exception {
      final String content = ""
          + "Belarus  (3042,544)  (3092,544)  (2991,542)\n"
          + "54 Sea Zone  (6071,2696)  (6123,2695)\n"
          + "Philippines (5267,2091)\n";

      final Map<String, List<Point>> pointListsByName =
          IoUtils.readFromMemory(content.getBytes(StandardCharsets.UTF_8), PointFileReaderWriter::readOneToMany);

      assertThat(pointListsByName, is(ImmutableMap.of(
          "Belarus", Arrays.asList(new Point(3042, 544), new Point(3092, 544), new Point(2991, 542)),
          "54 Sea Zone", Arrays.asList(new Point(6071, 2696), new Point(6123, 2695)),
          "Philippines", Arrays.asList(new Point(5267, 2091)))));
    }
  }
}
