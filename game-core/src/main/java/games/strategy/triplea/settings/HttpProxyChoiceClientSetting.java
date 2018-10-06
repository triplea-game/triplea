package games.strategy.triplea.settings;

import java.util.logging.Level;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.framework.system.HttpProxy;
import lombok.extern.java.Log;

/**
 * Implementation of {@link ClientSetting} for values of type
 * {@link games.strategy.engine.framework.system.HttpProxy.ProxyChoice}.
 */
@Log
public final class HttpProxyChoiceClientSetting extends ClientSetting {
  HttpProxyChoiceClientSetting(final String name, final HttpProxy.ProxyChoice value) {
    super(name, value.toString());
  }

  public HttpProxy.ProxyChoice defaultProxyChoiceValue() {
    return parseEncodedValue(defaultValue);
  }

  public HttpProxy.ProxyChoice proxyChoiceValue() {
    return parseEncodedValue(value());
  }

  @VisibleForTesting
  static HttpProxy.ProxyChoice parseEncodedValue(final String encodedValue) {
    try {
      return HttpProxy.ProxyChoice.valueOf(encodedValue);
    } catch (final IllegalArgumentException e) {
      log.log(Level.WARNING, "Illegal proxy choice: '" + encodedValue + "'", e);
      return HttpProxy.ProxyChoice.NONE;
    }
  }
}
