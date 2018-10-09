package org.triplea.server.http.spark;

import org.triplea.server.ServerConfiguration;
import org.triplea.server.http.spark.controller.ControllerConfiguration;
import org.triplea.server.http.spark.controller.SparkController;

import com.google.common.annotations.VisibleForTesting;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Main entry point for firing up a spark server. This will launch an http server.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SparkServer {

  public static void main(final String[] args) {
    start(ServerConfiguration.prod());
  }

  @VisibleForTesting
  static void start(final ServerConfiguration environmentConfiguration) {
    new ControllerConfiguration(environmentConfiguration)
        .getControllers()
        .forEach(SparkController::run);
  }
}
