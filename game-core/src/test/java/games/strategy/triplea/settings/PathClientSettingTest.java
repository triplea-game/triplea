package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.file.Paths;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class PathClientSettingTest {
  private final PathClientSetting clientSetting = new PathClientSetting("name", Paths.get("/path", "to", "file"));

  @Nested
  final class FormatValueTest {
    @Test
    void shouldReturnEncodedValue() {
      assertThat(clientSetting.formatValue(Paths.get("/absolute", "path", "to", "file")), is("/absolute/path/to/file"));
      assertThat(clientSetting.formatValue(Paths.get("relative", "path", "to", "file")), is("relative/path/to/file"));
    }
  }

  @Nested
  final class ParseValueTest {
    @Test
    void shouldReturnPath() {
      assertThat(clientSetting.parseValue("/absolute/path/to/file"), is(Paths.get("/absolute", "path", "to", "file")));
      assertThat(clientSetting.parseValue("relative/path/to/file"), is(Paths.get("relative", "path", "to", "file")));
    }
  }
}
