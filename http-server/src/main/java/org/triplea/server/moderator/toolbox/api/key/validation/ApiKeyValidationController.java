package org.triplea.server.moderator.toolbox.api.key.validation;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;

import lombok.AllArgsConstructor;

/**
 * Controller used to explicitly validate a moderator API key. We expect this to be used when launching
 * the moderator toolbox window. If the key presented is not valid then the user will need to be prompted
 * for a valid key.
 * Note: we still need to do API key validation on any other http server side method that accepts and uses
 * a moderator API key as an attacker could invoke those endpoints directly in either a brute-force cracking
 * attack or a DDOS attack.
 */
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
