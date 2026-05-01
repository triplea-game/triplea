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
 *
 * <p>Usage:
 *
 * <pre>
 *   try (CloseableHttpClient client = NodeBbHttpClients.builder().build()) { ... }
 *   try (CloseableHttpClient client =
 *       NodeBbHttpClients.builder().bearerToken(token).build()) { ... }
 * </pre>
 */
final class NodeBbHttpClients {
  private NodeBbHttpClients() {}

  static Builder builder() {
    return new Builder();
  }

  static final class Builder {
    private String bearerToken;

    private Builder() {}

    Builder bearerToken(final String bearerToken) {
      this.bearerToken = bearerToken;
      return this;
    }

    CloseableHttpClient build() {
      final List<Header> headers = new ArrayList<>();
      headers.add(
          new BasicHeader("User-Agent", "triplea/" + ProductVersionReader.getCurrentVersion()));
      if (bearerToken != null) {
        headers.add(new BasicHeader("Authorization", "Bearer " + bearerToken));
      }
      return HttpClients.custom().setDefaultHeaders(headers).disableCookieManagement().build();
    }
  }
}
