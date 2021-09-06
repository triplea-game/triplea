package org.triplea.spitfire.server.controllers.user.account;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.lobby.login.LobbyLoginClient;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.http.client.lobby.login.LoginRequest;
import org.triplea.modules.chat.Chatters;
import org.triplea.modules.user.account.login.LoginModule;
import org.triplea.spitfire.server.HttpController;

@Builder
@AllArgsConstructor
public class LoginController extends HttpController {
  @Nonnull private final LoginModule loginModule;

  public static LoginController build(final Jdbi jdbi, final Chatters chatters) {
    return LoginController.builder() //
        .loginModule(LoginModule.build(jdbi, chatters))
        .build();
  }

  @POST
  @Path(LobbyLoginClient.LOGIN_PATH)
  public LobbyLoginResponse login(
      @Context final HttpServletRequest request, final LoginRequest loginRequest) {
    Preconditions.checkArgument(loginRequest != null);
    Preconditions.checkArgument(loginRequest.getName() != null);
    return loginModule.doLogin(
        loginRequest,
        request.getHeader(AuthenticationHeaders.SYSTEM_ID_HEADER),
        request.getRemoteAddr());
  }
}
