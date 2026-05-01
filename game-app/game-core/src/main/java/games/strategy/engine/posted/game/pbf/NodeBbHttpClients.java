package games.strategy.engine.posted.game.pbf;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
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

  /**
   * Returns a new HTTP client with the standard NodeBB default headers attached.
   *
   * @param bearerToken Authorization bearer to attach as a default header. Pass {@code null} for
   *     pre-auth requests (e.g. login / token generation).
   */
  static CloseableHttpClient newClient(@Nullable final String bearerToken) {
    final String version = ProductVersionReader.getCurrentVersion().toString();
    final List<Header> defaultHeaders = new ArrayList<>(2);
    defaultHeaders.add(new BasicHeader("User-Agent", "triplea/" + version));
    if (bearerToken != null) {
      defaultHeaders.add(new BasicHeader("Authorization", "Bearer " + bearerToken));
    }
    return HttpClients.custom().setDefaultHeaders(defaultHeaders).disableCookieManagement().build();
  }
}
