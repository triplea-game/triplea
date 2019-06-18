package org.triplea.server.moderator.toolbox.api.key.registration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;
import org.triplea.http.client.moderator.toolbox.register.key.RegisterApiKeyResult;
import org.triplea.http.client.moderator.toolbox.register.key.ToolboxRegisterNewKeyClient;
import org.triplea.server.moderator.toolbox.api.key.exception.ApiKeyLockOutException;
import org.triplea.server.moderator.toolbox.api.key.exception.IncorrectApiKeyException;

import com.google.common.base.Preconditions;

import lombok.AllArgsConstructor;

/**
 * Controller providing access for moderators to 'register' a single-use-key, a key given to them by moderator admins,
 * and in return the endpoint returns a new API key for the moderator to use from then-on.
 */
@Path("")
@AllArgsConstructor
public class ApiKeyRegistrationController {

  private final ApiKeyRegistrationService apiKeyRegistrationService;

  /**
   * This method is used to consume a single use key and new password from user. If the single
   * use api-key is valid then the backend will generate a new key and salt it with the given password.
   * The backend will return the new to the user and store a hash of the salted password.
   * If the single-use key is not valid, a 400 is returned.
   */
  @POST
  @Path(ToolboxRegisterNewKeyClient.REGISTER_API_KEY_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response registerApiKey(@Context final HttpServletRequest request) {
    Preconditions.checkNotNull(request.getHeader(ToolboxHttpHeaders.API_KEY_HEADER));
    Preconditions.checkNotNull(request.getHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER));
    try {
      final String newApiKey = apiKeyRegistrationService.registerKey(
          request,
          request.getHeader(ToolboxHttpHeaders.API_KEY_HEADER),
          request.getHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER));

      return Response
          .status(200)
          .entity(RegisterApiKeyResult.newApiKeyResult(newApiKey))
          .build();
    } catch (final IncorrectApiKeyException | ApiKeyLockOutException | PasswordTooEasyException e) {
      return Response
          .status(400)
          .entity(RegisterApiKeyResult.newErrorResult(e.getMessage()))
          .build();
    }
  }
}
