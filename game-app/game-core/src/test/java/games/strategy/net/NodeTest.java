package games.strategy.net;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.java.IpAddressParser;

class NodeTest {

  @NonNls private static final String NAME = "name";
  @NonNls private static final String IP = "99.99.99.99";
  private static final int PORT = 3000;

  @Test
  @DisplayName("Verify data from InetSocketAddress is preserved on construction")
  void verifyConstructionUsingInetSocketAddress() {
    final Node node = new Node(NAME, InetSocketAddress.createUnresolved(IP, PORT));

    assertThat(node.getName()).isEqualTo(NAME);
    assertThat(node.getAddress()).isEqualTo(IpAddressParser.fromString(IP));
    assertThat(node.getIpAddress()).isEqualTo(IP);
    assertThat(node.getPort()).isEqualTo(PORT);
  }

  @Test
  @DisplayName("Verify we can use hostnames")
  void verifyConstructionUsingHostName() {
    final Node node = new Node(NAME, InetSocketAddress.createUnresolved("localhost", PORT));

    assertThat(node.getAddress()).isEqualTo(IpAddressParser.fromString("localhost"));
  }

  @Test
  @DisplayName("Verify socket address is well constructed when returned")
  void verifyRetrievalOfSocketAddress() {
    final InetSocketAddress address =
        new Node(NAME, InetSocketAddress.createUnresolved(IP, PORT)).getSocketAddress();

    assertThat(address.getAddress()).isEqualTo(IpAddressParser.fromString(IP));
    assertThat(address.getPort()).isEqualTo(PORT);
  }
}
