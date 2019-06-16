package org.triplea.server.http;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.dropwizard.testing.DropwizardTestSupport;

/**
 * This class starts a drop wizard server before all tests and then shuts it down afterwards.
 * Note, if a server is already running, then that server is used. (That is a hack to support
 * Travis builds, when the server is started from in-test, on travis, the output is suppressed.
 * By starting a server externally and then using that, in that case the server output does
 * appear in the Travis build log.)
 */
public class DropwizardServerExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
  private static boolean started = false;

  private static final DropwizardTestSupport<AppConfig> support =
      new DropwizardTestSupport<>(ServerApplication.class, "configuration-prerelease.yml");

  @Override
  public void beforeAll(final ExtensionContext context) {
    if (!started) {
      started = true;
      try {
        support.before();
      } catch (final RuntimeException e) {
        // ignore, server is already started
      }
      context.getRoot().getStore(GLOBAL).put("dropwizard-startup", this);
    }
  }

  @Override
  public void close() {
    support.after();
  }
}
