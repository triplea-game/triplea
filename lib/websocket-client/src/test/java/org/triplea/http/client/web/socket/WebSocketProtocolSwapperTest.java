package org.triplea.http.client.web.socket;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebSocketProtocolSwapperTest {

  @Test
  @DisplayName("Verify 'https' protocol is swapped to 'wss'")
  void swapHttpsProtocol() {
    final URI inputUri = URI.create("https://uri.com");
    final URI updated = new WebSocketProtocolSwapper().apply(inputUri);
    assertThat(updated).isEqualTo(URI.create("wss://uri.com"));
  }

  @Test
  @DisplayName("Verify 'http' protocol is swapped to 'ws'")
  void swapHttpProtocol() {
    final URI inputUri = URI.create("http://uri.com");
    final URI updated = new WebSocketProtocolSwapper().apply(inputUri);
    assertThat(updated).isEqualTo(URI.create("ws://uri.com"));
  }
}
