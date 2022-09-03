package org.triplea.spitfire.server.controllers.user.account;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.dropwizard.common.IpAddressExtractor;
import org.triplea.http.client.forgot.password.ForgotPasswordClient;
import org.triplea.http.client.forgot.password.ForgotPasswordRequest;
import org.triplea.http.client.forgot.password.ForgotPasswordResponse;
import org.triplea.modules.LobbyModuleConfig;
import org.triplea.modules.forgot.password.ForgotPasswordModule;
import org.triplea.spitfire.server.HttpController;

/** Http controller that binds the error upload endpoint with the error report upload handler. */
@Builder
@AllArgsConstructor(
    access = AccessLevel.PACKAGE,
    onConstructor_ = {@VisibleForTesting})
public class ForgotPasswordController extends HttpController {
  @Nonnull private final BiFunction<String, ForgotPasswordRequest, String> forgotPasswordModule;

  public static ForgotPasswordController build(
      final LobbyModuleConfig lobbyModuleConfig, final Jdbi jdbi) {
    return ForgotPasswordController.builder()
        .forgotPasswordModule(
            ForgotPasswordModule.build(
                lobbyModuleConfig.isGameHostConnectivityCheckEnabled(), jdbi))
        .build();
  }

  @POST
  @Path(ForgotPasswordClient.FORGOT_PASSWORD_PATH)
  public ForgotPasswordResponse requestTempPassword(
      @Context final HttpServletRequest request,
      final ForgotPasswordRequest forgotPasswordRequest) {

    if (forgotPasswordRequest.getUsername() == null || forgotPasswordRequest.getEmail() == null) {
      throw new IllegalArgumentException("Missing username or email in request");
    }

    return ForgotPasswordResponse.builder()
        .responseMessage(
            forgotPasswordModule.apply(
                IpAddressExtractor.extractIpAddress(request), forgotPasswordRequest))
        .build();
  }
}
