package org.triplea.http.client;

import java.net.URI;

import com.google.common.base.Preconditions;

import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import lombok.extern.java.Log;

@Log
final class FeignFactory {
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

  private FeignFactory() {}

  static <T> T build(final Class<T> classType, final URI hostUri) {
    Preconditions.checkNotNull(classType);
    Preconditions.checkNotNull(hostUri);

    return Feign.builder()
        .encoder(gsonEncoder)
        .decoder(gsonDecoder)
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
}
