package games.strategy.triplea.settings;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class PathClientSettingTest {
  private final PathClientSetting clientSetting =
      new PathClientSetting("name", Path.of("/path", "to", "file"));

  @Nested
  final class EncodeValueTest {
    @Test
    void shouldReturnEncodedValue() {
      final String separator = File.separator;
      assertThat(clientSetting.encodeValue(Path.of("/absolute", "path", "to", "file")))
          .isEqualTo(
              String.format(
                  "%sabsolute%spath%sto%sfile", separator, separator, separator, separator));
      assertThat(clientSetting.encodeValue(Path.of("relative", "path", "to", "file")))
          .isEqualTo(String.format("relative%spath%sto%sfile", separator, separator, separator));
    }
  }

  @Nested
  final class DecodeValueTest {
    @Test
    void shouldReturnPath() {
      assertThat(clientSetting.decodeValue("/absolute/path/to/file"))
          .isEqualTo(Path.of("/absolute", "path", "to", "file"));
      assertThat(clientSetting.decodeValue("relative/path/to/file"))
          .isEqualTo(Path.of("relative", "path", "to", "file"));
    }
  }
}
