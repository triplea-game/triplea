package org.triplea.server.http.spark;

import java.util.Optional;
import java.util.logging.Level;

import org.triplea.server.ServerConfiguration;
import org.triplea.server.http.spark.controller.ControllerConfiguration;

import com.google.common.annotations.VisibleForTesting;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

/** Main entry point for firing up a spark server. This will launch an http server. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Log
public final class SparkServer {

  public static void main(final String[] args) {
    try {
      start(ServerConfiguration.prod());
    } catch (final Error e) {
      log.log(
          Level.SEVERE,
          String.format(
              "Server crash: %s",
              Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse(e.getMessage())));
      throw e;
    }
  }

  @VisibleForTesting
  static void start(final ServerConfiguration serverConfiguration) {
    ControllerConfiguration.getControllers(serverConfiguration).forEach(Runnable::run);
  }
}
