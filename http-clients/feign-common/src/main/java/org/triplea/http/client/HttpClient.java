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
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds http feign clients, each feign interface class should have a static {@code newClient}
 * convenience method to construct an instance of this class which can be used to interact with the
 * remote http interface.
 *
 * @param <ClientTypeT> The feign client interface, should be an interface type that has feign
 *     annotations on it.
 */
@Slf4j
@RequiredArgsConstructor
@AllArgsConstructor
public class HttpClient<ClientTypeT> implements Supplier<ClientTypeT> {

  private static final Decoder gsonDecoder = JsonDecoder.gsonDecoder();
  /** How long we can take to start receiving a message. */
  private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5 * 1000;
  /** How long we can spend receiving a message. */
  private static final int DEFAULT_READ_TIME_OUT_MS = 20 * 1000;

  @Nonnull private final Class<ClientTypeT> classType;
  @Nonnull private final URI hostUri;

  private Integer maxAttempts = 3;

  @Override
  public ClientTypeT get() {
    Preconditions.checkNotNull(classType);
    Preconditions.checkNotNull(hostUri);

    return Feign.builder()
        .encoder(new JsonEncoder())
        .decoder(gsonDecoder)
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
