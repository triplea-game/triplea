package org.triplea.modules.user.account.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.SystemIdHeader;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.http.client.lobby.login.LoginRequest;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

  private static final String IP = "ipAddress";
  private static final String SYSTEM_ID = "system-id";

  @Mock private HttpServletRequest httpServletRequest;
  @Mock private LoginRequest loginRequest;
  @Mock private LobbyLoginResponse lobbyLoginResponse;

  @Mock private LoginModule loginModule;

  private LoginController loginController;

  @BeforeEach
  void setup() {
    loginController = new LoginController(loginModule);
  }

  @Test
  void login() {
    when(httpServletRequest.getRemoteAddr()).thenReturn(IP);
    when(httpServletRequest.getHeader(SystemIdHeader.SYSTEM_ID_HEADER)).thenReturn(SYSTEM_ID);
    when(loginModule.doLogin(loginRequest, SYSTEM_ID, IP)).thenReturn(lobbyLoginResponse);

    final LobbyLoginResponse response = loginController.login(httpServletRequest, loginRequest);

    assertThat(response, sameInstance(lobbyLoginResponse));
  }
}
