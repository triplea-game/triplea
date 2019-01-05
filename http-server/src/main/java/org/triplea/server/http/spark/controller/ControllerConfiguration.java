package org.triplea.server.http.spark.controller;

import java.util.Collections;

import org.triplea.server.ServerConfiguration;

import lombok.AllArgsConstructor;

/** Creates the full set of controllers used by the spark server. */
@AllArgsConstructor
public class ControllerConfiguration {

  public static Iterable<Runnable> getControllers(final ServerConfiguration serverConfiguration) {
    return Collections.singletonList(
        new ErrorReportController(serverConfiguration.getErrorUploader()));
  }
}
