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
  private final ApiKeyValidationService apiKeyValidationService;

  @POST
  @Path(ModeratorToolboxClient.VALIDATE_API_KEY_PATH)
  public Response validateApiKey(@Context final HttpServletRequest request) {
    apiKeyValidationService.verifyApiKey(request);
    return Response.status(200).entity(ModeratorToolboxClient.SUCCESS).build();
  }
}
