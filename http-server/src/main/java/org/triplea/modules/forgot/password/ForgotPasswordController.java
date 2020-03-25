package org.triplea.modules.forgot.password;

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
import org.triplea.http.HttpController;
import org.triplea.http.client.forgot.password.ForgotPasswordClient;
import org.triplea.http.client.forgot.password.ForgotPasswordRequest;
import org.triplea.http.client.forgot.password.ForgotPasswordResponse;

/** Http controller that binds the error upload endpoint with the error report upload handler. */
@Builder
@AllArgsConstructor(
    access = AccessLevel.PACKAGE,
    onConstructor_ = {@VisibleForTesting})
public class ForgotPasswordController extends HttpController {
  @Nonnull private final BiFunction<String, ForgotPasswordRequest, String> forgotPasswordModule;

  @POST
  @Path(ForgotPasswordClient.FORGOT_PASSWORD_PATH)
  public ForgotPasswordResponse requestTempPassword(
      @Context final HttpServletRequest request,
      final ForgotPasswordRequest forgotPasswordRequest) {

    if (forgotPasswordRequest.getUsername() == null || forgotPasswordRequest.getEmail() == null) {
      throw new IllegalArgumentException("Missing username or email in request");
    }

    return ForgotPasswordResponse.builder()
        .responseMessage(forgotPasswordModule.apply(request.getRemoteAddr(), forgotPasswordRequest))
        .build();
  }
}
