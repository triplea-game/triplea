package games.strategy.engine.framework.system;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.GameRunner;
import org.apache.commons.httpclient.HostConfiguration;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class HttpProxy {
  public enum ProxyChoice {
    NONE, USE_SYSTEM_SETTINGS, USE_USER_PREFERENCES
  }

  public static final String HTTP_PROXYHOST = "http.proxyHost";
  public static final String HTTP_PROXYPORT = "http.proxyPort";
  public static final String PROXY_HOST = "proxy.host";
  public static final String PROXY_PORT = "proxy.port";
  public static final String PROXY_CHOICE = "proxy.choice";

  public static void setupProxies() {
    // System properties, not user pref
    String proxyHostArgument = System.getProperty(PROXY_HOST);
    String proxyPortArgument = System.getProperty(PROXY_PORT);
    if (proxyHostArgument == null) {
      // in case it was set by -D we also check this
      proxyHostArgument = System.getProperty(HTTP_PROXYHOST);
    }
    if (proxyPortArgument == null) {
      proxyPortArgument = System.getProperty(HTTP_PROXYPORT);
    }
    // arguments should override and set user preferences
    String proxyHost = null;
    if (proxyHostArgument != null && proxyHostArgument.trim().length() > 0) {
      proxyHost = proxyHostArgument;
    }
    String proxyPort = null;
    if (proxyPortArgument != null && proxyPortArgument.trim().length() > 0) {
      try {
        Integer.parseInt(proxyPortArgument);
        proxyPort = proxyPortArgument;
      } catch (final NumberFormatException e) {
        ClientLogger.logQuietly(e);
      }
    }
    if (proxyHost != null || proxyPort != null) {
      setProxy(proxyHost, proxyPort, ProxyChoice.USE_USER_PREFERENCES);
    }
    final Preferences pref = Preferences.userNodeForPackage(GameRunner.class);
    final ProxyChoice choice = ProxyChoice.valueOf(pref.get(PROXY_CHOICE, ProxyChoice.NONE.toString()));
    if (choice == ProxyChoice.USE_SYSTEM_SETTINGS) {
      setToUseSystemProxies();
    } else if (choice == ProxyChoice.USE_USER_PREFERENCES) {
      final String host = pref.get(PROXY_HOST, "");
      final String port = pref.get(PROXY_PORT, "");
      if (host.trim().length() > 0) {
        System.setProperty(HTTP_PROXYHOST, host);
      }
      if (port.trim().length() > 0) {
        System.setProperty(HTTP_PROXYPORT, port);
      }
    }
  }




  public static void setProxy(final String proxyHost, final String proxyPort, final ProxyChoice proxyChoice) {
    final Preferences pref = Preferences.userNodeForPackage(GameRunner.class);
    final ProxyChoice choice;
    if (proxyChoice != null) {
      choice = proxyChoice;
      pref.put(PROXY_CHOICE, proxyChoice.toString());
    } else {
      choice = ProxyChoice.valueOf(pref.get(PROXY_CHOICE, ProxyChoice.NONE.toString()));
    }
    if (proxyHost != null && proxyHost.trim().length() > 0) {
      // user pref, not system properties
      pref.put(PROXY_HOST, proxyHost);
      if (choice == ProxyChoice.USE_USER_PREFERENCES) {
        System.setProperty(HTTP_PROXYHOST, proxyHost);
      }
    }
    if (proxyPort != null && proxyPort.trim().length() > 0) {
      try {
        Integer.parseInt(proxyPort);
        // user pref, not system properties
        pref.put(PROXY_PORT, proxyPort);
        if (choice == ProxyChoice.USE_USER_PREFERENCES) {
          System.setProperty(HTTP_PROXYPORT, proxyPort);
        }
      } catch (final NumberFormatException e) {
        ClientLogger.logQuietly(e);
      }
    }
    if (choice == ProxyChoice.NONE) {
      System.clearProperty(HTTP_PROXYHOST);
      System.clearProperty(HTTP_PROXYPORT);
    } else if (choice == ProxyChoice.USE_SYSTEM_SETTINGS) {
      setToUseSystemProxies();
    }
    if (proxyHost != null || proxyPort != null || proxyChoice != null) {
      try {
        pref.flush();
        pref.sync();
      } catch (final BackingStoreException e) {
        ClientLogger.logQuietly(e);
      }
    }
  }

  private static void setToUseSystemProxies() {
    final String javaNetUseSystemProxies = "java.net.useSystemProxies";
    System.setProperty(javaNetUseSystemProxies, "true");
    List<java.net.Proxy> proxyList = null;
    try {
      final ProxySelector def = ProxySelector.getDefault();
      if (def != null) {
        proxyList = def.select(new URI("http://sourceforge.net/"));
        ProxySelector.setDefault(null);
        if (proxyList != null && !proxyList.isEmpty()) {
          final java.net.Proxy proxy = proxyList.get(0);
          final InetSocketAddress address = (InetSocketAddress) proxy.address();
          if (address != null) {
            final String host = address.getHostName();
            final int port = address.getPort();
            System.setProperty(HTTP_PROXYHOST, host);
            System.setProperty(HTTP_PROXYPORT, Integer.toString(port));
            System.setProperty(PROXY_HOST, host);
            System.setProperty(PROXY_PORT, Integer.toString(port));
          } else {
            System.clearProperty(HTTP_PROXYHOST);
            System.clearProperty(HTTP_PROXYPORT);
            System.clearProperty(PROXY_HOST);
            System.clearProperty(PROXY_PORT);
          }
        }
      } else {
        final String host = System.getProperty(PROXY_HOST);
        final String port = System.getProperty(PROXY_PORT);
        if (host == null) {
          System.clearProperty(HTTP_PROXYHOST);
        } else {
          System.setProperty(HTTP_PROXYHOST, host);
        }
        if (port == null) {
          System.clearProperty(HTTP_PROXYPORT);
        } else {
          try {
            Integer.parseInt(port);
            System.setProperty(HTTP_PROXYPORT, port);
          } catch (final NumberFormatException nfe) {
            // nothing
          }
        }
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    } finally {
      System.setProperty(javaNetUseSystemProxies, "false");
    }
  }

  public static void addProxy(final HostConfiguration config) {
    final String host = System.getProperty(HTTP_PROXYHOST);
    final String port = System.getProperty(HTTP_PROXYPORT, "-1");
    if (host != null && host.trim().length() > 0) {
      config.setProxy(host, Integer.valueOf(port));
    }
  }

}
