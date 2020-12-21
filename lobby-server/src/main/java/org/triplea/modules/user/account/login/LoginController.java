package org.triplea.modules.user.account.login;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.HttpController;
import org.triplea.http.client.SystemIdHeader;
import org.triplea.http.client.lobby.login.LobbyLoginClient;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.http.client.lobby.login.LoginRequest;
import org.triplea.modules.chat.Chatters;

@Builder
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
    Preconditions.checkNotNull(loginRequest);
    return loginModule.doLogin(
        loginRequest, request.getHeader(SystemIdHeader.SYSTEM_ID_HEADER), request.getRemoteAddr());
  }
}
