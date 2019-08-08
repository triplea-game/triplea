package org.triplea.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.triplea.ResourceLoader;
import java.net.URL;
import org.junit.jupiter.api.Test;

class LocalizeHtmlTest {
  private final ResourceLoader loader = mock(ResourceLoader.class);
  private final String testHtml =
      "<audio src='test-audio'> &lt;img src=&quot;test&quot;&gt;"
          + "<img useless fill src=\"dir/actual-link\" alt='Alternative Text' > "
          + "<p>  Placeholder </P> <img\n"
          + " src='another-link.png' />";

  @Test
  void testLocalizeHtml() throws Exception {

    when(loader.getResource("doc/images/actual-link")).thenReturn(new URL("http://local-link-1"));
    when(loader.getResource("doc/images/another-link.png"))
        .thenReturn(new URL("http://local-link-2"));

    final String result = LocalizeHtml.localizeImgLinksInHtml(testHtml, loader);

    assertThat(result, not(containsString("dir/actual-link")));
    assertThat(result, not(containsString("another-link.png")));

    assertThat(result, containsString("http://local-link-1"));
    assertThat(result, containsString("http://local-link-2"));

    assertThat(result, containsString("test-audio"));

    verify(loader, times(2)).getResource(any());
  }

  @Test
  void testFallbackLink() throws Exception {
    when(loader.getResource("doc/images/actual-link")).thenReturn(null);
    when(loader.getResource("doc/images/another-link.png")).thenReturn(null);
    when(loader.getResource("doc/images/notFound.png"))
        .thenReturn(null, new URL("http://notFound.png"));
    final String result = LocalizeHtml.localizeImgLinksInHtml(testHtml, loader);
    assertThat(result, containsString("dir/actual-link"));
    assertThat(result, containsString("http://notFound.png"));
    assertThat(result, not(containsString("another-link.png")));
  }

  @Test
  void testDoNothingWithoutResources() {
    assertThat(LocalizeHtml.localizeImgLinksInHtml(testHtml, loader), is(equalTo(testHtml)));
  }

  @Test
  void testDoNothingWithoutTag() {
    final String testHtml = "<p>Paragraph</p>";
    assertThat(LocalizeHtml.localizeImgLinksInHtml(testHtml, loader), is(equalTo(testHtml)));
    verify(loader, never()).getResource(any());
  }

  @Test
  void testCaching() throws Exception {
    final String testHtml = "<img src='test'><img src='test'>";
    when(loader.getResource("doc/images/test")).thenReturn(new URL("http://test"));
    LocalizeHtml.localizeImgLinksInHtml(testHtml, loader);
    verify(loader, times(1)).getResource(any());
  }

  @Test
  void testStrictHtml() {
    final String testHtml = "<img src=\"test\"";
    assertThat(LocalizeHtml.localizeImgLinksInHtml(testHtml, loader), is(equalTo(testHtml)));
    verify(loader, never()).getResource(any());
  }
}
