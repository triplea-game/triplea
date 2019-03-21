package org.triplea.http.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

/**
 * Builds http feign clients, each feign interface class should have a static {@code newClient} convenience method
 * to construct an instance of this class which can be used to interact with the remote http interface.
 *
 * @param <ClientTypeT> The feign client interface, should be an interface type that has feign annotations on it.
 */
@Log
@RequiredArgsConstructor
public class HttpClient<ClientTypeT> implements Supplier<ClientTypeT> {

  private static final Encoder gsonEncoder = new GsonEncoder();
  private static final Decoder gsonDecoder = new GsonDecoder();
  /**
   * How long we can take to start receiving a message.
   */
  private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5 * 1000;
  /**
   * How long we can spend receiving a message.
   */
  private static final int DEFAULT_READ_TIME_OUT_MS = 20 * 1000;

  @Nonnull
  private final Class<ClientTypeT> classType;
  @Nonnull
  private final URI hostUri;

  @Override
  public ClientTypeT get() {
    Preconditions.checkNotNull(classType);
    Preconditions.checkNotNull(hostUri);

    return Feign.builder()
        .encoder(gsonEncoder)
        .decoder(gsonDecoder)
        .errorDecoder(HttpClient::errorDecoder)
        .logger(new Logger() {
          @Override
          protected void log(final String configKey, final String format, final Object... args) {
            log.info(configKey + ": " + String.format(format, args));
          }
        })
        .logLevel(Logger.Level.BASIC)
        .options(new Request.Options(DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIME_OUT_MS))
        .target(classType, hostUri.toString());
  }

  /**
   * This decoder acts similar to the default decoder where the method key and response status codes
   * are printed, but in addition, if present, any server response body message is also printed.
   */
  private static Exception errorDecoder(final String methodKey, final Response response) {

    final String firstLine = String.format("Status %s reading %s", response.status(), methodKey);

    throw Optional.ofNullable(response.body())
        .map(body -> {
          try (BufferedReader reader = new BufferedReader(body.asReader())) {
            final String errorMessageBody = reader.lines().collect(Collectors.joining("\n"));
            return new HttpCommunicationException(response.status(), firstLine + "\n" + errorMessageBody);
          } catch (final IOException e) {
            log.log(Level.INFO, "An additional error occurred when decoding response", e);
            return new HttpCommunicationException(response.status(), firstLine);
          }
        })
        .orElseGet(() -> new HttpCommunicationException(response.status(), firstLine));
  }
}
