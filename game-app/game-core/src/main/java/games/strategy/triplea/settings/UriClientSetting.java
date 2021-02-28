package games.strategy.triplea.settings;

import java.net.URI;
import java.net.URISyntaxException;

final class UriClientSetting extends ClientSetting<URI> {
  protected UriClientSetting(final String name) {
    super(URI.class, name);
  }

  protected UriClientSetting(final String name, final URI defaultValue) {
    super(URI.class, name, defaultValue);
  }

  @Override
  protected String encodeValue(final URI value) {
    return value.toString();
  }

  @Override
  protected URI decodeValue(final String encodedValue) throws ValueEncodingException {
    try {
      return new URI(encodedValue);
    } catch (final URISyntaxException e) {
      throw new ValueEncodingException(e);
    }
  }
}
