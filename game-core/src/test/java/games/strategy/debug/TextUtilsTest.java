package games.strategy.debug;

import static games.strategy.debug.TextUtils.textToHtml;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class TextUtilsTest {
  @Nested
  final class TextToHtmlTest {
    @Test
    void shouldEmbedTextInHtmlElement() {
      assertThat(textToHtml("test"), is("<html>test</html>"));
    }

    @Test
    void shouldEscapeHtmlMetacharacters() {
      assertThat(textToHtml("abc<>&\"'123"), is("<html>abc&lt;&gt;&amp;&quot;&#39;123</html>"));
    }

    @Test
    void shouldReplaceLineSeparatorsWithBreakElement() {
      assertThat(
          textToHtml("a" + System.lineSeparator() + "b" + System.lineSeparator() + "c"),
          is("<html>a<br/>b<br/>c</html>"));
    }
  }
}
