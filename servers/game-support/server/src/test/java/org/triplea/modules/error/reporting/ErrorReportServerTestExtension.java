package org.triplea.modules.error.reporting;

import io.dropwizard.testing.DropwizardTestSupport;
import org.triplea.server.error.reporting.ErrorReportingConfiguration;
import org.triplea.server.error.reporting.ErrorReportingServer;
import org.triplea.test.common.RequiresDatabase;
import org.triplea.test.support.DropwizardServerExtension;

/**
 * Use with {@code @ExtendWith(SpitfireServerTestExtension.class)} Tests extended with this
 * extension will launch a drop wizard server when the test starts up. If a server is already
 * running at the start of a test, it will be re-used.
 */
@RequiresDatabase
public class ErrorReportServerTestExtension
    extends DropwizardServerExtension<ErrorReportingConfiguration> {

  private static final DropwizardTestSupport<ErrorReportingConfiguration> testSupport =
      new DropwizardTestSupport<>(ErrorReportingServer.class, "configuration.yml");

  @Override
  public DropwizardTestSupport<ErrorReportingConfiguration> getSupport() {
    return testSupport;
  }
}
