package org.triplea.http.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.domain.data.ApiKey;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

@SuppressWarnings("PrivateConstructorForUtilityClass")
@ExtendWith({WiremockResolver.class, WiremockUriResolver.class})
public abstract class WireMockTest {
  protected static final ApiKey API_KEY = ApiKey.of("api-key-value");

  protected static <T> T newClient(
      final WireMockServer wireMockServer, final BiFunction<URI, ApiKey, T> factoryFunction) {
    final URI hostUri = buildHostUri(wireMockServer);
    return factoryFunction.apply(hostUri, API_KEY);
  }

  protected static <T> T newClient(
      final WireMockServer wireMockServer, final Function<URI, T> factoryFunction) {
    final URI hostUri = buildHostUri(wireMockServer);
    return factoryFunction.apply(hostUri);
  }

  protected static URI buildHostUri(final WireMockServer wireMockServer) {
    return URI.create(wireMockServer.url(""));
  }
}
