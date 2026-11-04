package games.strategy.engine.framework.system;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import java.net.InetSocketAddress;
import java.net.Proxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class HttpProxyTest extends AbstractClientSettingTestCase {
  @DisplayName("Map previews go through the proxy set in the settings, like map downloads")
  @Test
  void setProxyIsUsed() {
    ClientSetting.proxyHost.setValue("proxy.example.com");
    ClientSetting.proxyPort.setValue(8080);

    final Proxy proxy = HttpProxy.getProxy();

    assertThat(proxy.address())
        .as("a network that needs the proxy blocks direct connections")
        .isEqualTo(InetSocketAddress.createUnresolved("proxy.example.com", 8080));
  }

  @DisplayName("Without a proxy in the settings, connections go direct")
  @Test
  void noProxyWithoutSettings() {
    assertThat(HttpProxy.getProxy()).isEqualTo(Proxy.NO_PROXY);
  }
}
