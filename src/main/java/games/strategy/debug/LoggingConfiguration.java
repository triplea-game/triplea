package games.strategy.debug;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.LogManager;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.framework.system.SystemProperties;

/**
 * Provides support for configuring the Java platform's core logging facilities.
 */
public final class LoggingConfiguration {
  private LoggingConfiguration() {}

  /**
   * Initializes the application logging configuration.
   *
   * <p>
   * The default application logging configuration is read from the {@code logging.properties} file located in this
   * package. The logging configuration can be overridden in production using the standard {@link LogManager} system
   * properties "java.util.logging.config.class" or "java.util.logging.config.file".
   * </p>
   */
  public static void initialize() {
    initialize(LogManager.getLogManager(), SystemProperties.all());
  }

  @VisibleForTesting
  static void initialize(final LogManager logManager, final Properties systemProperties) {
    if (systemProperties.containsKey(SystemPropertyKeys.LOGGING_CONFIGURATION_CLASS_NAME)
        || systemProperties.containsKey(SystemPropertyKeys.LOGGING_CONFIGURATION_FILE_NAME)) {
      return;
    }

    try (InputStream is = LoggingConfiguration.class.getResourceAsStream("logging.properties")) {
      logManager.readConfiguration(is);
    } catch (final IOException e) {
      System.err.println("unable to set custom logging configuration using logging.properties");
      System.err.println(e);
    }
  }

  @VisibleForTesting
  interface SystemPropertyKeys {
    String LOGGING_CONFIGURATION_CLASS_NAME = "java.util.logging.config.class";
    String LOGGING_CONFIGURATION_FILE_NAME = "java.util.logging.config.file";
  }
}
