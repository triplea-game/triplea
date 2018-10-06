package games.strategy.triplea.settings;

import java.util.logging.Level;

import games.strategy.engine.framework.system.HttpProxy;
import lombok.extern.java.Log;

@Log
final class HttpProxyChoiceClientSetting extends ClientSetting<HttpProxy.ProxyChoice> {
  HttpProxyChoiceClientSetting(final String name, final HttpProxy.ProxyChoice defaultValue) {
    super(name, defaultValue);
  }

  @Override
  protected String formatValue(final HttpProxy.ProxyChoice value) {
    return value.toString();
  }

  @Override
  protected HttpProxy.ProxyChoice parseValue(final String encodedValue) {
    try {
      return HttpProxy.ProxyChoice.valueOf(encodedValue);
    } catch (final IllegalArgumentException e) {
      log.log(Level.WARNING, "Illegal HTTP proxy choice: '" + encodedValue + "'", e);
      return HttpProxy.ProxyChoice.NONE;
    }
  }
}
