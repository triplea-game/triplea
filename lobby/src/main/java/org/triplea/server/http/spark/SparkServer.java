package org.triplea.server.http.spark;

import org.triplea.server.ServerConfiguration;
import org.triplea.server.http.spark.controller.ControllerConfiguration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

/** Main entry point for firing up a spark server. This will launch an http server. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Log
public final class SparkServer {

  public static void start(final ServerConfiguration serverConfiguration) {
    ControllerConfiguration.getControllers(serverConfiguration).forEach(Runnable::run);
  }
}
