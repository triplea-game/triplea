package org.triplea.modules.game;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ApiKey;
import org.triplea.modules.access.authentication.AuthenticatedUser;
import org.triplea.modules.game.ConnectivityCheck.ReverseConnectionResult;

@ExtendWith(MockitoExtension.class)
class ConnectivityControllerTest {
  private static final AuthenticatedUser AUTHENTICATED_USER =
      AuthenticatedUser.builder().userRole("user-role").apiKey(ApiKey.of("api-key")).build();

  @Mock private ConnectivityCheck connectivityCheck;

  @InjectMocks private ConnectivityController connectivityController;

  @Test
  void gameNotFoundReturns422() {
    when(connectivityCheck.canDoReverseConnect(AUTHENTICATED_USER.getApiKey(), "game-id"))
        .thenReturn(ReverseConnectionResult.GAME_ID_NOT_FOUND);

    final Response result = connectivityController.checkConnectivity(AUTHENTICATED_USER, "game-id");

    assertThat(result.getStatus(), is(HttpStatus.UNPROCESSABLE_ENTITY_422));
  }

  @Test
  void cannotConnectReturnsFalse() {
    when(connectivityCheck.canDoReverseConnect(AUTHENTICATED_USER.getApiKey(), "game-id"))
        .thenReturn(ReverseConnectionResult.FAILED);

    final Response result = connectivityController.checkConnectivity(AUTHENTICATED_USER, "game-id");

    assertThat(result.getStatus(), is(HttpStatus.OK_200));
    assertThat(result.getEntity(), is(false));
  }

  @Test
  void canConnectReturnsTrue() {
    when(connectivityCheck.canDoReverseConnect(AUTHENTICATED_USER.getApiKey(), "game-id"))
        .thenReturn(ReverseConnectionResult.SUCCESS);

    final Response result = connectivityController.checkConnectivity(AUTHENTICATED_USER, "game-id");

    assertThat(result.getStatus(), is(HttpStatus.OK_200));
    assertThat(result.getEntity(), is(true));
  }
}
