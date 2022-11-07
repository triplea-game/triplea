package org.triplea.spitfire.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import com.google.common.base.Preconditions;
import feign.FeignException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.SystemIdLoader;
import org.triplea.http.client.LobbyHttpClientConfig;

@ExtendWith(SpitfireServerTestExtension.class)
@ExtendWith(SpitfireDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@DataSet(value = ControllerIntegrationTest.DATA_SETS, useSequenceFiltering = false)
public abstract class ControllerIntegrationTest {

  @BeforeAll
  static void setup() {
    LobbyHttpClientConfig.setConfig(
        LobbyHttpClientConfig.builder()
            .clientVersion("1.0")
            .systemId(SystemIdLoader.load().getValue())
            .build());
  }

  @AfterAll
  static void tearDown() {
    LobbyHttpClientConfig.setConfig(null);
  }

  /**
   * All data sets used for controller integration tests. Note, we include all data even that which
   * is not used because the tests rely on just a single static data set. If we were doing more
   * thorough DB level testing we would want different variations of the same data. In this case we
   * include everything to keep test configuration easy, notably so each test does not need to
   * define the data it needs.
   */
  public static final String DATA_SETS =
      "user_role.yml,"
          + "lobby_user.yml,"
          + "lobby_api_key.yml,"
          + "access_log.yml,"
          + "bad_word.yml,"
          + "banned_username.yml,"
          + "banned_user.yml,"
          + "game_hosting_api_key.yml,"
          + "map_index.yml,"
          + "map_tag_value.yml,"
          + "moderator_action_history.yml,"
          + "temp_password_request.yml"
          + "";

  public static final ApiKey ADMIN = ApiKey.of("ADMIN");
  public static final ApiKey MODERATOR = ApiKey.of("MODERATOR");
  public static final ApiKey PLAYER = ApiKey.of("PLAYER");
  public static final ApiKey ANONYMOUS = ApiKey.of("ANONYMOUS");
  public static final ApiKey HOST = ApiKey.of("HOST");

  public static final List<ApiKey> NOT_MODERATORS = List.of(ANONYMOUS, PLAYER);
  public static final List<ApiKey> NOT_HOST = List.of(ANONYMOUS, PLAYER, ADMIN, MODERATOR);

  /**
   * Loops over a collection of api keys each one being used to create a client, we then iterate
   * over each invocation and invoke it using each client. For each invocation we verify we get an
   * {@code HttpInteractionException} with status '403' (not authorized).
   *
   * @param keys The API keys expected to not have authorization
   * @param clientBuilder Function taking an API key and returning a client
   * @param clientInvocations Consumer collection that accepts a client we constructed and should do
   *     an API call where we would expect a 403 using that client.
   */
  public static <T> void assertNotAuthorized(
      final Collection<ApiKey> keys,
      final Function<ApiKey, T> clientBuilder,
      final Consumer<T>... clientInvocations) {
    Preconditions.checkArgument(!keys.isEmpty());
    Preconditions.checkArgument(clientInvocations.length > 0);

    for (final ApiKey key : keys) {
      final T client = clientBuilder.apply(key);

      for (final Consumer<T> invocation : clientInvocations) {
        try {
          invocation.accept(client);
          fail("Invocation did not produce a 403");
        } catch (final FeignException feignException) {
          assertThat(feignException.status(), is(403));
        }
      }
    }
  }

  public static void assertBadRequest(final Runnable invocation) {
    try {
      invocation.run();
      fail("Invocation did not produce a 400");
    } catch (final FeignException feignException) {
      assertThat(feignException.status(), is(400));
    }
  }
}
