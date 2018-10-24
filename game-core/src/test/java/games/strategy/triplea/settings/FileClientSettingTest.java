package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class FileClientSettingTest {
  private final FileClientSetting clientSetting = new FileClientSetting("name", new File("/path/to/file"));

  @Nested
  final class FormatValueTest {
    @Test
    void shouldReturnEncodedValue() {
      assertThat(clientSetting.formatValue(new File("/absolute/path/to/file")), is("/absolute/path/to/file"));
      assertThat(clientSetting.formatValue(new File("relative/path/to/file")), is("relative/path/to/file"));
    }
  }

  @Nested
  final class ParseValueTest {
    @Test
    void shouldReturnFile() {
      assertThat(clientSetting.parseValue("/absolute/path/to/file"), is(new File("/absolute/path/to/file")));
      assertThat(clientSetting.parseValue("relative/path/to/file"), is(new File("relative/path/to/file")));
    }
  }
}
