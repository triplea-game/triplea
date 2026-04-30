package org.triplea.http;

import java.util.Map;
import java.util.function.Supplier;
import org.apache.http.client.methods.HttpRequestBase;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Applies a standard set of identifying headers to outbound HTTP requests built with Apache
 * HttpClient. The header values are produced by a {@link Supplier} registered once at app startup
 * via {@link #setProvider(Supplier)}.
 *
 * <p>Until the provider is set, {@link #apply(HttpRequestBase)} attaches {@code Triplea-Version:
 * Unknown} and {@code User-Agent: triplea/Unknown}. The default keeps the headers <em>present</em>
 * (which is what header-presence-based proxy/WAF rules require) without forcing every entry point
 * to register a provider.
 */
public final class HttpClientHeaders {
  public static final String VERSION_HEADER = "Triplea-Version";
  public static final String USER_AGENT_HEADER = "User-Agent";

  private static final Map<String, String> UNKNOWN_HEADERS =
      Map.of(VERSION_HEADER, "Unknown", USER_AGENT_HEADER, "triplea/Unknown");

  private static volatile Supplier<Map<String, String>> provider = () -> UNKNOWN_HEADERS;

  private HttpClientHeaders() {}

  /**
   * Registers the supplier of standard headers. Intended to be called once early in app startup
   * (e.g. alongside {@code LobbyHttpClientConfig.setConfig(...)}).
   */
  public static void setProvider(final Supplier<Map<String, String>> headerProvider) {
    provider = headerProvider;
  }

  /**
   * Convenience for callers that have a TripleA engine version in hand. Registers a provider that
   * emits {@code Triplea-Version: <version>} and {@code User-Agent: triplea/<version>}.
   */
  public static void setVersion(final String version) {
    final Map<String, String> headers =
        Map.of(VERSION_HEADER, version, USER_AGENT_HEADER, "triplea/" + version);
    provider = () -> headers;
  }

  /** Adds the standard headers from the registered provider to the given request. */
  public static void apply(final HttpRequestBase request) {
    provider.get().forEach(request::addHeader);
  }

  @VisibleForTesting
  public static void resetForTesting() {
    provider = () -> UNKNOWN_HEADERS;
  }
}
