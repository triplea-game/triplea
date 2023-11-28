package org.triplea.http.client;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.FormatMethod;
import feign.Feign;
import feign.FeignException;
import feign.Logger;
import feign.Request;
import feign.Response;
import feign.Retryer;
import feign.codec.Decoder;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds http Feign clients, each Feign interface class should have a static {@code newClient}
 * convenience method to construct an instance of this class which can be used to interact with the
 * remote http interface.
 *
 * @param <ClientTypeT> Feign client interface, should be an interface type that contains Feign.
 *     annotations on it.
 */
@Slf4j
public class HttpClient<ClientTypeT> implements Supplier<ClientTypeT> {

  private static final Decoder gsonDecoder = JsonDecoder.gsonDecoder();

  /**
   * Allowed idle time for a connection with no activity (waiting to receive a message). Expressed
   * in milliseconds. Default 5 seconds.
   */
  private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;

  /**
   * The time a connection should allow for completely receiving a message. Expressed in
   * milliseconds. Default 20 seconds.
   */
  private static final int DEFAULT_READ_TIME_OUT_MS = 20000;

  private static final Integer maxAttempts = 3;

  private final Class<ClientTypeT> classType;
  private final URI hostUri;
  private final Map<String, String> headers;

  public HttpClient(Class<ClientTypeT> classType, URI hostUri, Map<String, String> headers) {
    this.classType = classType;
    this.hostUri = hostUri;
    this.headers = headers;
  }

  public static <T> T newClient(Class<T> t, URI uri) {
    return new HttpClient<>(t, uri, Map.of()).get();
  }

  public static <T> T newClient(Class<T> t, URI uri, Map<String, String> headers) {
    return new HttpClient<>(t, uri, headers).get();
  }

  @Override
  public ClientTypeT get() {
    Preconditions.checkNotNull(classType);
    Preconditions.checkNotNull(hostUri);

    return Feign.builder()
        .encoder(new JsonEncoder())
        .decoder(gsonDecoder)
        .requestInterceptor(
            requestTemplate -> {
              headers.forEach(requestTemplate::header);
              requestTemplate.header("Content-Type", "application/json");
              requestTemplate.header("Accept", "application/json");
            })
        .errorDecoder(FeignException::errorStatus)
        .retryer(new Retryer.Default(100, SECONDS.toMillis(1), maxAttempts))
        .logger(
            new Logger() {
              @Override
              protected Response logAndRebufferResponse(
                  final String configKey,
                  final Level logLevel,
                  final Response response,
                  final long elapsedTime)
                  throws IOException {
                // Do not log 'sendKeepAlive' requests (very noisy)
                return configKey.contains("sendKeepAlive")
                    ? response
                    : super.logAndRebufferResponse(configKey, logLevel, response, elapsedTime);
              }

              @FormatMethod
              @Override
              protected void log(
                  final String configKey, final String format, final Object... args) {
                final String logMessage = String.format(format, args);
                log.trace(configKey + ": " + logMessage);
              }
            })
        .logLevel(Logger.Level.BASIC)
        .options(
            new Request.Options(
                DEFAULT_CONNECT_TIMEOUT_MS,
                TimeUnit.MILLISECONDS,
                DEFAULT_READ_TIME_OUT_MS,
                TimeUnit.MILLISECONDS,
                true))
        .target(classType, hostUri.toString());
  }
}
