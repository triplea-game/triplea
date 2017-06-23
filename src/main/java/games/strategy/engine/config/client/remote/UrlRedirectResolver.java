package games.strategy.engine.config.client.remote;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.common.annotations.VisibleForTesting;

/**
 * Module that follows URLs and any redirects and determines the final URL to use.
 * @deprecated swap HTTP redirect detection with newer HTTP tooling so we would follow multiple redirects
 */
class UrlRedirectResolver {

  private final UrlConnectionFactory urlConnectionFactory;

  UrlRedirectResolver() {
    this(new UrlConnectionFactory());
  }

  @VisibleForTesting
  UrlRedirectResolver(final UrlConnectionFactory urlConnectionFactory) {
    this.urlConnectionFactory = urlConnectionFactory;
  }

  String getUrlFollowingRedirects(final String possibleRedirectionUrl) throws IOException {
    URL url = new URL(possibleRedirectionUrl);

    final HttpURLConnection conn = urlConnectionFactory.openConnection(url);
    final int status = conn.getResponseCode();
    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
        || status == HttpURLConnection.HTTP_SEE_OTHER) {
      // update the URL if we were redirected
      url = new URL(conn.getHeaderField("Location"));
    }
    return url.toString();
  }


  static class UrlConnectionFactory {
    HttpURLConnection openConnection(final URL url) throws IOException {
      return (HttpURLConnection) url.openConnection();
    }
  }
}
