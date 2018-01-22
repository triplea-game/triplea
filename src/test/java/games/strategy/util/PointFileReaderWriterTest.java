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
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import games.strategy.io.IoUtils;

public final class PointFileReaderWriterTest {
  @Nested
  public final class ReadOneToOneTest {
    @Test
    public void shouldReturnEmptyMapWhenStreamIsNull() throws Exception {
      assertThat(PointFileReaderWriter.readOneToOne(null), is(Collections.emptyMap()));
    }

    @Test
    public void shouldNotCloseStream() throws Exception {
      final InputStream is = spy(new ByteArrayInputStream(new byte[0]));

      PointFileReaderWriter.readOneToOne(is);

      verify(is, never()).close();
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
}
