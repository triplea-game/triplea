package org.triplea.http.client.web.socket;

import java.net.URI;
import java.util.function.Function;

class WebSocketProtocolSwapper implements Function<URI, URI> {
  @Override
  public URI apply(final URI uri) {
    return uri.getScheme().matches("^https?$")
        ? URI.create(uri.toString().replaceFirst("^http", "ws"))
        : uri;
  }
}
