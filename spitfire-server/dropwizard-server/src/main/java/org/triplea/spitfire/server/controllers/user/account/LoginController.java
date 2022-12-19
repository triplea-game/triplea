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
import org.triplea.domain.data.LobbyConstants;
import org.triplea.dropwizard.common.IpAddressExtractor;
import org.triplea.http.client.LobbyHttpClientConfig;
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
    Preconditions.checkArgument(
        loginRequest.getName().length() <= LobbyConstants.USERNAME_MAX_LENGTH);
    Preconditions.checkArgument(
        loginRequest.getName().length() >= LobbyConstants.USERNAME_MIN_LENGTH);

    return loginModule.doLogin(
        loginRequest,
        request.getHeader(LobbyHttpClientConfig.SYSTEM_ID_HEADER),
        IpAddressExtractor.extractIpAddress(request));
  }
}
