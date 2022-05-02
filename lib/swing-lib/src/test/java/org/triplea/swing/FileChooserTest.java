package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
      assertThat(
          appendExtensionIfAbsent(Path.of("path/file.aaa"), "bbb"),
          is(Path.of("path/file.aaa.bbb")));
      assertThat(
          appendExtensionIfAbsent(Path.of("path/filebbb"), "bbb"), is(Path.of("path/filebbb.bbb")));
    }

    @Test
    void shouldNotAppendExtensionWhenExtensionPresent() {
      assertThat(
          appendExtensionIfAbsent(Path.of("path/file.bbb"), "bbb"), is(Path.of("path/file.bbb")));
    }

    @Test
    void shouldHandleExtensionThatStartsWithPeriod() {
      assertThat(
          appendExtensionIfAbsent(Path.of("path/file.aaa"), ".bbb"),
          is(Path.of("path/file.aaa.bbb")));
    }

    @Test
    void shouldUseCaseInsensitiveComparisonForExtension() {
      assertThat(
          appendExtensionIfAbsent(Path.of("path/file.bBb"), "BbB"), is(Path.of("path/file.bBb")));
    }
  }

  @Nested
  final class ExtensionWithLeadingPeriodTest {
    @Test
    void shouldReturnExtensionWithLeadingPeriod() {
      assertThat(extensionWithLeadingPeriod(""), is(""));

      assertThat(extensionWithLeadingPeriod("a"), is(".a"));
      assertThat(extensionWithLeadingPeriod(".a"), is(".a"));

      assertThat(extensionWithLeadingPeriod("aa"), is(".aa"));
      assertThat(extensionWithLeadingPeriod(".aa"), is(".aa"));

      assertThat(extensionWithLeadingPeriod("aaa"), is(".aaa"));
      assertThat(extensionWithLeadingPeriod(".aaa"), is(".aaa"));

      assertThat(extensionWithLeadingPeriod("aaa.aaa"), is(".aaa.aaa"));
      assertThat(extensionWithLeadingPeriod(".aaa.aaa"), is(".aaa.aaa"));
    }
  }

  @Nested
  final class ExtensionWithoutLeadingPeriodTest {
    @Test
    void shouldReturnExtensionWithoutLeadingPeriod() {
      assertThat(extensionWithoutLeadingPeriod(""), is(""));

      assertThat(extensionWithoutLeadingPeriod("a"), is("a"));
      assertThat(extensionWithoutLeadingPeriod(".a"), is("a"));

      assertThat(extensionWithoutLeadingPeriod("aa"), is("aa"));
      assertThat(extensionWithoutLeadingPeriod(".aa"), is("aa"));

      assertThat(extensionWithoutLeadingPeriod("aaa"), is("aaa"));
      assertThat(extensionWithoutLeadingPeriod(".aaa"), is("aaa"));

      assertThat(extensionWithoutLeadingPeriod("aaa.aaa"), is("aaa.aaa"));
      assertThat(extensionWithoutLeadingPeriod(".aaa.aaa"), is("aaa.aaa"));
    }
  }
}
