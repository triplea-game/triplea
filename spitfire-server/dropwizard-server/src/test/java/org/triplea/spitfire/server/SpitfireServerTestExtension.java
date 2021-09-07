package org.triplea.spitfire.server;

import io.dropwizard.testing.DropwizardTestSupport;

/**
 * Use with {@code @ExtendWith(SpitfireServerTestExtension.class)} Tests extended with this
 * extension will launch a drop wizard server when the test starts up. If a server is already
 * running at the start of a test, it will be re-used.
 */
public class SpitfireServerTestExtension extends DropwizardServerExtension<SpitfireServerConfig> {

  private static final DropwizardTestSupport<SpitfireServerConfig> testSupport =
      new DropwizardTestSupport<>(SpitfireServerApplication.class, "configuration.yml");

  @Override
  public DropwizardTestSupport<SpitfireServerConfig> getSupport() {
    return testSupport;
  }
}
