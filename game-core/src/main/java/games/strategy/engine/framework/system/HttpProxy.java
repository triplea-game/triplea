package games.strategy.engine.framework.system;

import com.google.common.base.Strings;
import games.strategy.triplea.settings.ClientSetting;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import javax.annotation.Nullable;
import lombok.extern.java.Log;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;

/** Provides methods to configure the proxy to use for HTTP requests. */
@Log
public final class HttpProxy {
  private HttpProxy() {}

  /**
   * Set of possible proxy options. Users can change between these via settings. System preferences
   * will load proxy settings from the OS.
   */
  public enum ProxyChoice {
    NONE,
    USE_SYSTEM_SETTINGS,
    USE_USER_PREFERENCES
  }

  public static boolean isUsingSystemProxy() {
    return ClientSetting.proxyChoice.getValueOrThrow().equals(ProxyChoice.USE_SYSTEM_SETTINGS);
  }

  /** Get the latest system proxy settings and apply them. */
  public static void updateSystemProxy() {
    final Optional<InetSocketAddress> address = getSystemProxy();
    final @Nullable String host;
    final @Nullable Integer port;
    if (address.isEmpty()) {
      host = null;
      port = null;
    } else {
      host = Strings.nullToEmpty(address.get().getHostName()).trim();
      port = host.isEmpty() ? null : address.get().getPort();
    }

    ClientSetting.proxyHost.setValue(host);
    ClientSetting.proxyPort.setValue(port);
    ClientSetting.flush();
  }

  private static Optional<InetSocketAddress> getSystemProxy() {
    // this property is temporarily needed to turn on proxying
    SystemProperties.setJavaNetUseSystemProxies("true");
    try {
      final ProxySelector def = ProxySelector.getDefault();
      if (def != null) {
        // TODO: if we switch to HTTPS, we will potentially need an https URL, proxies can very by
        // protocol.
        final String anyUrlThatShouldAvailable = "http://sourceforge.net/";
        final List<Proxy> proxyList = def.select(new URI(anyUrlThatShouldAvailable));
        ProxySelector.setDefault(null);
        if (proxyList != null && !proxyList.isEmpty()) {
          final Proxy proxy = proxyList.get(0);
          final InetSocketAddress address = (InetSocketAddress) proxy.address();
          return Optional.ofNullable(address);
        }
      }
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Failed to get system HTTP proxy", e);
    } finally {
      SystemProperties.setJavaNetUseSystemProxies("false");
    }
    return Optional.empty();
  }

  /** Attaches proxy host and port values, if any, to the http request parameter. */
  public static void addProxy(final HttpRequestBase request) {
    if (ClientSetting.proxyHost.isSet() && ClientSetting.proxyPort.isSet()) {
      request.setConfig(
          RequestConfig.copy(request.getConfig())
              .setProxy(
                  new HttpHost(
                      ClientSetting.proxyHost.getValueOrThrow(),
                      ClientSetting.proxyPort.getValueOrThrow()))
              .build());
    }
  }
}
