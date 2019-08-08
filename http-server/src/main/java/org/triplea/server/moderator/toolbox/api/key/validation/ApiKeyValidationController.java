package org.triplea.server.moderator.toolbox.api.key.validation;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import org.triplea.http.client.moderator.toolbox.api.key.ToolboxApiKeyClient;

/**
 * Controller used to explicitly validate a moderator API key. We expect this to be used when
 * launching the moderator toolbox window. If the key presented is not valid then the user will need
 * to be prompted for a valid key. Note: we still need to do API key validation on any other http
 * server side method that accepts and uses a moderator API key as an attacker could invoke those
 * endpoints directly in either a brute-force cracking attack or a DDOS attack.
 */
@Path("")
@AllArgsConstructor
public class ApiKeyValidationController {
  private final ApiKeyValidationService apiKeyValidationService;

  @POST
  @Path(ToolboxApiKeyClient.VALIDATE_API_KEY_PATH)
  public Response validateApiKey(@Context final HttpServletRequest request) {
    apiKeyValidationService.verifyApiKey(request);
    return Response.ok().build();
  }

  @GET
  @Path(ToolboxApiKeyClient.RESET_LOCKOUTS_PATH)
  public Response clearLockouts(@Context final HttpServletRequest request) {
    apiKeyValidationService.clearLockoutCache(request);
    return Response.ok().build();
  }
}
