package org.triplea.server.moderator.toolbox.api.key;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.triplea.http.client.moderator.toolbox.NewApiKey;
import org.triplea.http.client.moderator.toolbox.api.key.ApiKeyData;
import org.triplea.http.client.moderator.toolbox.api.key.ToolboxApiKeyClient;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

/**
 * Controller used to support the backend for the 'API-Key Tab'. Provides endpoint for a moderator
 * to view their keys, delete keys, and issue themselves a new single-use-key.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Builder
public class ApiKeyController {

  private final ApiKeyValidationService apiKeyValidationService;
  private final GenerateSingleUseKeyService generateSingleUseKeyService;
  private final ApiKeyService apiKeyService;

  @POST
  @Path(ToolboxApiKeyClient.GENERATE_SINGLE_USE_KEY_PATH)
  public Response generateSingleUseKey(@Context final HttpServletRequest request) {
    final int moderatorUserId = apiKeyValidationService.lookupModeratorIdByApiKey(request);
    final String newKey = generateSingleUseKeyService.generateSingleUseKey(moderatorUserId);
    return Response.ok().entity(new NewApiKey(newKey)).build();
  }

  @GET
  @Path(ToolboxApiKeyClient.GET_API_KEYS)
  public Response getApiKeys(@Context final HttpServletRequest request) {
    final int moderatorUserId = apiKeyValidationService.lookupModeratorIdByApiKey(request);
    final List<ApiKeyData> keyData = apiKeyService.getKeys(moderatorUserId);
    return Response.ok().entity(keyData).build();
  }

  @POST
  @Path(ToolboxApiKeyClient.DELETE_API_KEY)
  public Response deleteApiKey(
      @Context final HttpServletRequest request, final String publicKeyId) {
    apiKeyValidationService.verifyApiKey(request);
    apiKeyService.deleteKey(publicKeyId);
    return Response.ok().build();
  }
}
