package games.strategy.engine.posted.game.pbf;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.triplea.config.product.ProductVersionReader;

/**
 * Builds {@link CloseableHttpClient} instances configured for talking to NodeBB forum APIs.
 *
 * <p>Each client carries a {@code User-Agent: triplea/<version>} default header identifying TripleA
 * so legitimate forum traffic can be distinguished from anonymous bot traffic by the forum's WAF
 * (e.g. for whitelisting via a Cloudflare rule).
 */
final class NodeBbHttpClients {
  private NodeBbHttpClients() {}

  /** Returns a new HTTP client for unauthenticated NodeBB requests (e.g. login / token mint). */
  static CloseableHttpClient newPreAuthClient() {
    final List<Header> headers = createHeaders();
    return HttpClients.custom().setDefaultHeaders(headers).disableCookieManagement().build();
  }

  /**
   * Returns a new HTTP client for authenticated NodeBB requests. The bearer token is attached as a
   * default {@code Authorization} header.
   */
  static CloseableHttpClient newPostAuthClient(final String bearerToken) {
    final List<Header> headers = createHeaders();
    headers.add(new BasicHeader("Authorization", "Bearer " + bearerToken));
    return HttpClients.custom().setDefaultHeaders(headers).disableCookieManagement().build();
  }

  private static List<Header> createHeaders() {
    final String version = ProductVersionReader.getCurrentVersion().toString();
    final List<Header> headers = new ArrayList<>();
    headers.add(new BasicHeader("User-Agent", "triplea/" + version));
    return headers;
  }
}
