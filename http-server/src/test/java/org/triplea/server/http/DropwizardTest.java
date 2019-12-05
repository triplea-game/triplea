package org.triplea.server.http;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import io.dropwizard.testing.DropwizardTestSupport;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.triplea.test.common.Integration;

/** Core configuration for a test that will start a dropwizard server and initialize database. */
@Integration
@DataSet(cleanBefore = true, value = "integration.yml")
@ExtendWith(DBUnitExtension.class)
@ExtendWith(DropwizardTest.DropwizardServerExtension.class)
@SuppressWarnings("PrivateConstructorForUtilityClass")
public abstract class DropwizardTest {
  protected final URI localhost = URI.create("http://localhost:8080");

  /**
   * Extension to start a drop wizard server before all tests and then shuts it down afterwards.
   * Note, if a server is already running, then that server is used.
   */
  public static class DropwizardServerExtension
      implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
    private static AtomicBoolean started = new AtomicBoolean(false);

    private static final DropwizardTestSupport<AppConfig> support =
        new DropwizardTestSupport<>(ServerApplication.class, "configuration-testing.yml");

    @Override
    public void beforeAll(final ExtensionContext context) {
      if (started.compareAndSet(false, true)) {
        try {
          support.before();
        } catch (final RuntimeException e) {
          // ignore, server is already started
        }
        // register this extension so 'close' will be called after all tests execute
        context.getRoot().getStore(GLOBAL).put("dropwizard-startup", this);
      }
    }

    @Override
    public void close() {
      support.after();
    }
  }
}
