package games.strategy.debug;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Properties;
import java.util.logging.LogManager;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
public final class LoggingConfigurationTest {
  @Spy
  private final LogManager logManager = LogManager.getLogManager();

  private final Properties systemProperties = new Properties();

  @Test
  public void initialize_ShouldDoNothingWhenLoggingConfigurationClassNameSystemPropertyIsPresent() throws Exception {
    systemProperties.setProperty(LoggingConfiguration.SystemPropertyKeys.LOGGING_CONFIGURATION_CLASS_NAME, "");

    LoggingConfiguration.initialize(logManager, systemProperties);

    verify(logManager, never()).readConfiguration(any());
  }

  @Test
  public void initialize_ShouldDoNothingWhenLoggingConfigurationFileNameSystemPropertyIsPresent() throws Exception {
    systemProperties.setProperty(LoggingConfiguration.SystemPropertyKeys.LOGGING_CONFIGURATION_FILE_NAME, "");

    LoggingConfiguration.initialize(logManager, systemProperties);

    verify(logManager, never()).readConfiguration(any());
  }

  @Test
  public void initialize_ShouldReadCustomConfigurationWhenUserDoesNotOverrideLoggingConfiguration() throws Exception {
    LoggingConfiguration.initialize(logManager, systemProperties);

    verify(logManager).readConfiguration(any());
  }
}
