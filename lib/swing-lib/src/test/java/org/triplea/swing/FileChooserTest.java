package org.triplea.swing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.swing.FileChooser.appendExtensionIfAbsent;
import static org.triplea.swing.FileChooser.extensionWithLeadingPeriod;
import static org.triplea.swing.FileChooser.extensionWithoutLeadingPeriod;

import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class FileChooserTest {
  @Nested
  final class AppendExtensionIfAbsentTest {
    @Test
    void shouldAppendExtensionWhenExtensionAbsent() {
      assertThat(appendExtensionIfAbsent(Path.of("path/file.aaa"), "bbb"))
          .isEqualTo(Path.of("path/file.aaa.bbb"));
      assertThat(appendExtensionIfAbsent(Path.of("path/filebbb"), "bbb"))
          .isEqualTo(Path.of("path/filebbb.bbb"));
    }

    @Test
    void shouldNotAppendExtensionWhenExtensionPresent() {
      assertThat(appendExtensionIfAbsent(Path.of("path/file.bbb"), "bbb"))
          .isEqualTo(Path.of("path/file.bbb"));
    }

    @Test
    void shouldHandleExtensionThatStartsWithPeriod() {
      assertThat(appendExtensionIfAbsent(Path.of("path/file.aaa"), ".bbb"))
          .isEqualTo(Path.of("path/file.aaa.bbb"));
    }

    @Test
    void shouldUseCaseInsensitiveComparisonForExtension() {
      assertThat(appendExtensionIfAbsent(Path.of("path/file.bBb"), "BbB"))
          .isEqualTo(Path.of("path/file.bBb"));
    }
  }

  @Nested
  final class ExtensionWithLeadingPeriodTest {
    @Test
    void shouldReturnExtensionWithLeadingPeriod() {
      assertThat(extensionWithLeadingPeriod("")).isEqualTo("");

      assertThat(extensionWithLeadingPeriod("a")).isEqualTo(".a");
      assertThat(extensionWithLeadingPeriod(".a")).isEqualTo(".a");

      assertThat(extensionWithLeadingPeriod("aa")).isEqualTo(".aa");
      assertThat(extensionWithLeadingPeriod(".aa")).isEqualTo(".aa");

      assertThat(extensionWithLeadingPeriod("aaa")).isEqualTo(".aaa");
      assertThat(extensionWithLeadingPeriod(".aaa")).isEqualTo(".aaa");

      assertThat(extensionWithLeadingPeriod("aaa.aaa")).isEqualTo(".aaa.aaa");
      assertThat(extensionWithLeadingPeriod(".aaa.aaa")).isEqualTo(".aaa.aaa");
    }
  }

  @Nested
  final class ExtensionWithoutLeadingPeriodTest {
    @Test
    void shouldReturnExtensionWithoutLeadingPeriod() {
      assertThat(extensionWithoutLeadingPeriod("")).isEqualTo("");

      assertThat(extensionWithoutLeadingPeriod("a")).isEqualTo("a");
      assertThat(extensionWithoutLeadingPeriod(".a")).isEqualTo("a");

      assertThat(extensionWithoutLeadingPeriod("aa")).isEqualTo("aa");
      assertThat(extensionWithoutLeadingPeriod(".aa")).isEqualTo("aa");

      assertThat(extensionWithoutLeadingPeriod("aaa")).isEqualTo("aaa");
      assertThat(extensionWithoutLeadingPeriod(".aaa")).isEqualTo("aaa");

      assertThat(extensionWithoutLeadingPeriod("aaa.aaa")).isEqualTo("aaa.aaa");
      assertThat(extensionWithoutLeadingPeriod(".aaa.aaa")).isEqualTo("aaa.aaa");
    }
  }
}
