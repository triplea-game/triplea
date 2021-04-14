package org.triplea.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.io.FileUtils;

class LocalizeHtmlTest {

  private final Path path = Path.of("/does/not/exist");

  @Test
  void testLocalizeHtml() {
    final String testHtml =
        "<audio src='test-audio'> &lt;img src=&quot;test&quot;&gt;"
            + "<img useless fill src=\"dir/actual-link\" alt='Alternative Text' > "
            + "<p>  Placeholder </P> <img\n"
            + " src='another-link.png'/><img src=\"another-link\"/>";

    final String result = LocalizeHtml.localizeImgLinksInHtml(testHtml, path);

    final String urlPrefix = FileUtils.toUrl(path).toString();

    assertThat(
        result,
        is(
            "<audio src='test-audio'> &lt;img src=&quot;test&quot;&gt;"
                + "<img useless fill src=\""
                + urlPrefix
                + "/doc/images/dir/actual-link\" "
                + "alt='Alternative Text' > <p>  Placeholder </P> <img\n"
                + " src='"
                + urlPrefix
                + "/doc/images/another-link.png'/>"
                + "<img src=\""
                + urlPrefix
                + "/doc/images/another-link\"/>"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "<img src=http://someurl/>",
        "<img src=\"http://someurl\"/>",
        "<img\nsrc=\"http://someurl\"/>",
        "<p>Paragraph</p>"
      })
  void testAbsoluteUrl(final String testHtml) {
    assertThat(
        LocalizeHtml.localizeImgLinksInHtml(testHtml, path),
        is(equalTo(testHtml)));
  }
}
