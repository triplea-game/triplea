package org.triplea.server.http.spark.controller;

import java.util.Arrays;

import org.triplea.server.ServerConfiguration;

import lombok.AllArgsConstructor;

/** Creates the full set of controllers used by the spark server. */
@AllArgsConstructor
public class ControllerConfiguration {

  public static Iterable<Runnable> getControllers(final ServerConfiguration serverConfiguration) {
    return Arrays.asList(
        new ErrorReportController(serverConfiguration.getErrorUploader()),
        new AnonymousUserLoginController(serverConfiguration.getAnonymousUserLogin()),
        new RegisteredUserLoginController(serverConfiguration.getRegisteredUserLogin()));
  }
}
