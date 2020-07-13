package org.triplea.debug;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.triplea.debug.TextUtils.textToHtml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class TextUtilsTest {
  @Nested
  final class TextToHtmlTest {
    @Test
    void shouldEmbedTextInHtmlElement() {
      assertThat(textToHtml("test"), is("test"));
    }

    @Test
    void shouldEscapeHtmlMetacharacters() {
      assertThat(textToHtml("abc<>&\"'123"), is("abc&lt;&gt;&amp;&quot;&#39;123"));
    }

    @Test
    void shouldReplaceLineSeparatorsWithBreakElement() {
      assertThat(
          textToHtml("a" + System.lineSeparator() + "b" + System.lineSeparator() + "c"),
          is("a<br/>b<br/>c"));
    }
  }
}
