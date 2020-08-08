package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.nio.file.Paths;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class PathClientSettingTest {
  private final PathClientSetting clientSetting =
      new PathClientSetting("name", Paths.get("/path", "to", "file"));

  @Nested
  final class EncodeValueTest {
    @Test
    void shouldReturnEncodedValue() {
      final String separator = File.separator;
      assertThat(
          clientSetting.encodeValue(Paths.get("/absolute", "path", "to", "file")),
          is(
              String.format(
                  "%sabsolute%spath%sto%sfile", separator, separator, separator, separator)));
      assertThat(
          clientSetting.encodeValue(Paths.get("relative", "path", "to", "file")),
          is(String.format("relative%spath%sto%sfile", separator, separator, separator)));
    }
  }

  @Nested
  final class DecodeValueTest {
    @Test
    void shouldReturnPath() {
      assertThat(
          clientSetting.decodeValue("/absolute/path/to/file"),
          is(Paths.get("/absolute", "path", "to", "file")));
      assertThat(
          clientSetting.decodeValue("relative/path/to/file"),
          is(Paths.get("relative", "path", "to", "file")));
    }
  }
}
