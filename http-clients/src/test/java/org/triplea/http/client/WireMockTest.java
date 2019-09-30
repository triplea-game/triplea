package org.triplea.http.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.util.function.BiFunction;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

@SuppressWarnings("PrivateConstructorForUtilityClass")
@ExtendWith({WiremockResolver.class, WiremockUriResolver.class})
public class WireMockTest {
  static final ApiKey API_KEY = ApiKey.of("api-fdsa key----api-----key-----api----key");

  protected static <T> T newClient(
      final WireMockServer wireMockServer, final BiFunction<URI, ApiKey, T> factoryFunction) {
    final URI hostUri = URI.create(wireMockServer.url(""));
    return factoryFunction.apply(hostUri, API_KEY);
  }
}
