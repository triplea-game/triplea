package games.strategy.engine.config.client.remote;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class UrlRedirectResolverTest {


  @Mock
  private UrlRedirectResolver.UrlConnectionFactory mockUrlConnectionFactory;

  @Mock
  private HttpURLConnection mockHttpUrlConnection;

  @Test
  public void testRedirect() throws Exception {
    final String fakeUrl = "http://123";
    final UrlRedirectResolver testObj = new UrlRedirectResolver(mockUrlConnectionFactory);
    when(mockUrlConnectionFactory.openConnection(new URL(fakeUrl))).thenReturn(mockHttpUrlConnection);
    when(mockHttpUrlConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_ACCEPTED);


    final String result = testObj.getUrlFollowingRedirects(fakeUrl);


    assertThat("no redirect, expect same URL to be returned",
        result, is(fakeUrl));
  }


  @Test
  public void testRedirectWithRedirectCases() throws Exception {

    final String fakeUrl = "http://123";
    final String newUrl = "http://abcNewUrl";

    when(mockUrlConnectionFactory.openConnection(new URL(fakeUrl))).thenReturn(mockHttpUrlConnection);
    when(mockHttpUrlConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_MOVED_PERM);
    when(mockHttpUrlConnection.getHeaderField("Location")).thenReturn(newUrl);

    final UrlRedirectResolver testObj = new UrlRedirectResolver(mockUrlConnectionFactory);
    final String result = testObj.getUrlFollowingRedirects(fakeUrl);

    assertThat("redirect, url should be updated from location http header field",
        result, is(newUrl));
  }
}
