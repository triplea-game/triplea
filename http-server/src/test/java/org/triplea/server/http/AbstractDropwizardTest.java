package org.triplea.server.http;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.net.URI;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.test.common.Integration;

@Integration
@DataSet(cleanBefore = true, value = "integration.yml")
@ExtendWith(DBUnitExtension.class)
@ExtendWith(DropwizardServerExtension.class)
public class AbstractDropwizardTest {

  private static final URI LOCALHOST = URI.create("http://localhost:8080");

  protected AbstractDropwizardTest() {}

  protected static <T> T newClient(final Function<URI, T> clientFunction) {
    return clientFunction.apply(LOCALHOST);
  }

  protected static <T> T newClient(final BiFunction<URI, String, T> clientFunction) {
    return clientFunction.apply(LOCALHOST, "test");
  }

  protected static <T> T newClientWithInvalidCreds(
      final BiFunction<URI, String, T> clientFunction) {
    return clientFunction.apply(LOCALHOST, "not-correct");
  }
}
