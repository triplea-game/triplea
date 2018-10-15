package games.strategy.triplea.settings;

import games.strategy.engine.framework.system.HttpProxy;

final class HttpProxyChoiceClientSetting extends ClientSetting<HttpProxy.ProxyChoice> {
  HttpProxyChoiceClientSetting(final String name, final HttpProxy.ProxyChoice defaultValue) {
    super(HttpProxy.ProxyChoice.class, name, defaultValue);
  }

  @Override
  protected String formatValue(final HttpProxy.ProxyChoice value) {
    return value.toString();
  }

  @Override
  protected HttpProxy.ProxyChoice parseValue(final String encodedValue) {
    return HttpProxy.ProxyChoice.valueOf(encodedValue);
  }
}
