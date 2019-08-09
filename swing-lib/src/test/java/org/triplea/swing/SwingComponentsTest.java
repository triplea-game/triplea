package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.triplea.swing.SwingComponents.appendExtensionIfAbsent;
import static org.triplea.swing.SwingComponents.extensionWithLeadingPeriod;
import static org.triplea.swing.SwingComponents.extensionWithoutLeadingPeriod;

import java.io.File;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class SwingComponentsTest {
  @Nested
  final class AppendExtensionIfAbsentTest {
    @Test
    void shouldAppendExtensionWhenExtensionAbsent() {
      assertThat(
          appendExtensionIfAbsent(new File("path/file.aaa"), "bbb"),
          is(new File("path/file.aaa.bbb")));
      assertThat(
          appendExtensionIfAbsent(new File("path/filebbb"), "bbb"),
          is(new File("path/filebbb.bbb")));
    }

    @Test
    void shouldNotAppendExtensionWhenExtensionPresent() {
      assertThat(
          appendExtensionIfAbsent(new File("path/file.bbb"), "bbb"), is(new File("path/file.bbb")));
    }

    @Test
    void shouldHandleExtensionThatStartsWithPeriod() {
      assertThat(
          appendExtensionIfAbsent(new File("path/file.aaa"), ".bbb"),
          is(new File("path/file.aaa.bbb")));
    }

    @Test
    void shouldUseCaseInsensitiveComparisonForExtension() {
      assertThat(
          appendExtensionIfAbsent(new File("path/file.bBb"), "BbB"), is(new File("path/file.bBb")));
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
