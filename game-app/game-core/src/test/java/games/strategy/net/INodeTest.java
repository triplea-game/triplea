package games.strategy.net;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.java.IpAddressParser;

class INodeTest {

  @Test
  @DisplayName(
      "Verify the IP address we pass in to instantiate an INode is the same "
          + "one returned by 'getByIpAddress'")
  void getIpAddress() {
    final INode node = new Node("name", IpAddressParser.fromString("123.2.1.1"), 300);

    final String result = node.getIpAddress();

    assertThat(result).isEqualTo("123.2.1.1");
  }
}
