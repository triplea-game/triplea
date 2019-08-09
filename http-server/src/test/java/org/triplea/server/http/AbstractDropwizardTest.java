package org.triplea.server.http;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.net.URI;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.moderator.toolbox.ApiKeyPassword;
import org.triplea.test.common.Integration;

@Integration
@DataSet("integration.yml")
@ExtendWith(DBUnitExtension.class)
@ExtendWith(DropwizardServerExtension.class)
public class AbstractDropwizardTest {

  private static final URI LOCALHOST = URI.create("http://localhost:8080");

  protected AbstractDropwizardTest() {}

  protected static <T> T newClient(final Function<URI, T> clientFunction) {
    return clientFunction.apply(LOCALHOST);
  }

  protected static <T> T newClient(final BiFunction<URI, ApiKeyPassword, T> clientFunction) {
    return clientFunction.apply(
        LOCALHOST, ApiKeyPassword.builder().password("test").apiKey("test").build());
  }

  protected static <T> T newClientWithInvalidCreds(
      final BiFunction<URI, ApiKeyPassword, T> clientFunction) {
    return clientFunction.apply(
        LOCALHOST, ApiKeyPassword.builder().password("not-correct").apiKey("guessing").build());
  }
}
