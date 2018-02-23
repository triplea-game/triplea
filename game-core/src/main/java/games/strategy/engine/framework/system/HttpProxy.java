package games.strategy.engine.framework.system;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;

import com.google.common.base.Strings;

import games.strategy.debug.ClientLogger;
import games.strategy.triplea.settings.ClientSetting;

public class HttpProxy {

  /**
   * Set of possible proxy options. Users can change between these via settings.
   * System preferences will load proxy settings from the OS.
   */
  public enum ProxyChoice {
    NONE, USE_SYSTEM_SETTINGS, USE_USER_PREFERENCES
  }

  public static final String PROXY_CHOICE = "proxy.choice";

  public static boolean isUsingSystemProxy() {
    return ClientSetting.PROXY_CHOICE.value().equals(ProxyChoice.USE_SYSTEM_SETTINGS.toString());
  }

  /**
   * Get the latest system proxy settings and apply them.
   */
  public static void updateSystemProxy() {
    final Optional<InetSocketAddress> address = getSystemProxy();

    final String host;
    final String port;

    if (!address.isPresent()) {
      host = "";
      port = "";
    } else {
      host = Strings.nullToEmpty(address.get().getHostName()).trim();
      port = host.isEmpty() ? "" : String.valueOf(address.get().getPort());
    }

    ClientSetting.PROXY_HOST.save(host);
    ClientSetting.PROXY_PORT.save(port);
    ClientSetting.flush();
  }

  private static Optional<InetSocketAddress> getSystemProxy() {
    // this property is temporarily needed to turn on proxying
    SystemProperties.setJavaNetUseSystemProxies("true");
    try {
      final ProxySelector def = ProxySelector.getDefault();
      if (def != null) {
        // TODO: if we switch to HTTPS, we will potentially need an https URL, proxies can very by protocol.
        final String anyUrlThatShouldAvailable = "http://sourceforge.net/";
        final List<Proxy> proxyList = def.select(new URI(anyUrlThatShouldAvailable));
        ProxySelector.setDefault(null);
        if ((proxyList != null) && !proxyList.isEmpty()) {
          final Proxy proxy = proxyList.get(0);
          final InetSocketAddress address = (InetSocketAddress) proxy.address();
          return Optional.ofNullable(address);
        }
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly("Failed to get system HTTP proxy", e);
    } finally {
      SystemProperties.setJavaNetUseSystemProxies("false");
    }
    return Optional.empty();
  }

  /**
   * Attaches proxy host and port values, if any, to the http request parameter.
   */
  public static void addProxy(final HttpRequestBase request) {
    final String host = ClientSetting.PROXY_HOST.value();
    final String port = ClientSetting.PROXY_PORT.value();

    if ((Strings.emptyToNull(host) != null) && (Strings.emptyToNull(port) != null)) {
      request.setConfig(RequestConfig
          .copy(request.getConfig())
          .setProxy(new HttpHost(host, Integer.parseInt(port)))
          .build());
    }
  }

}
