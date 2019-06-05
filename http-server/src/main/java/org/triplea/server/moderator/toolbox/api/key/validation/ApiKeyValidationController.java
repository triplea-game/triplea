package org.triplea.server.moderator.toolbox.api.key.validation;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;

import lombok.AllArgsConstructor;

@Path("")
@AllArgsConstructor
public class ApiKeyValidationController {
  private final ApiKeySecurityService apiKeySecurityService;
  private final ApiKeyValidationService apiKeyValidationService;

  @POST
  @Path(ModeratorToolboxClient.VALIDATE_API_KEY_PATH)
  public Response validateApiKey(@Context final HttpServletRequest request) {
    if (!apiKeySecurityService.allowValidation(request)) {
      return ApiKeyValidationService.LOCK_OUT_RESPONSE;
    }

    return apiKeyValidationService.lookupModeratorIdByApiKey(request).isPresent()
        ? Response.status(200).entity(ModeratorToolboxClient.SUCCESS).build()
        : ApiKeyValidationService.API_KEY_NOT_FOUND_RESPONSE;
  }
}
