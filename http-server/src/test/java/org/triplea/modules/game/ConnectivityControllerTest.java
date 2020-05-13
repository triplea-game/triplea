package org.triplea.modules.game;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ApiKey;
import org.triplea.modules.access.authentication.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class ConnectivityControllerTest {
  private static final AuthenticatedUser AUTHENTICATED_USER =
      AuthenticatedUser.builder().userRole("user-role").apiKey(ApiKey.of("api-key")).build();

  @Mock private ConnectivityCheck connectivityCheck;

  @InjectMocks private ConnectivityController connectivityController;

  @Test
  void gameNotFoundReturns400() {
    when(connectivityCheck.gameExists(AUTHENTICATED_USER.getApiKey(), "game-id")).thenReturn(false);

    final Response result = connectivityController.checkConnectivity(AUTHENTICATED_USER, "game-id");

    assertThat(result.getStatus(), is(400));
  }

  @Test
  void cannotConnectReturnsFalse() {
    when(connectivityCheck.gameExists(AUTHENTICATED_USER.getApiKey(), "game-id")).thenReturn(true);
    when(connectivityCheck.canDoReverseConnect(AUTHENTICATED_USER.getApiKey(), "game-id"))
        .thenReturn(false);

    final Response result = connectivityController.checkConnectivity(AUTHENTICATED_USER, "game-id");

    assertThat(result.getStatus(), is(200));
    assertThat(result.getEntity(), is(false));
  }

  @Test
  void canConnectReturnsTrue() {
    when(connectivityCheck.gameExists(AUTHENTICATED_USER.getApiKey(), "game-id")).thenReturn(true);
    when(connectivityCheck.canDoReverseConnect(AUTHENTICATED_USER.getApiKey(), "game-id"))
        .thenReturn(true);

    final Response result = connectivityController.checkConnectivity(AUTHENTICATED_USER, "game-id");

    assertThat(result.getStatus(), is(200));
    assertThat(result.getEntity(), is(true));
  }
}
