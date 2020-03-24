package org.triplea.modules.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.InetAddress;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InetExtractorTest {

  private static final String IP_ADDRESS = "127.0.0.1";

  @Test
  void verify() throws Exception {
    final Map<String, Object> propertiesMap =
        Map.of(InetExtractor.IP_ADDRESS_KEY, "/" + IP_ADDRESS + ":42840");

    assertThat(InetExtractor.extract(propertiesMap), is(InetAddress.getByName(IP_ADDRESS)));
  }
}
