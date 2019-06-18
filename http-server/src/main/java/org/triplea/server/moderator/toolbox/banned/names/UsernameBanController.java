package org.triplea.server.moderator.toolbox.banned.names;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.triplea.http.client.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

import com.google.common.base.Preconditions;

import lombok.Builder;

/**
 * Endpoint for use by moderators to view, add and remove player username bans.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Builder
public class UsernameBanController {
  @Nonnull
  private final UsernameBanService bannedNamesService;
  @Nonnull
  private final ApiKeyValidationService apiKeyValidationService;

  @POST
  @Path(ToolboxUsernameBanClient.REMOVE_BANNED_USER_NAME_PATH)
  public Response removeBannedUsername(@Context final HttpServletRequest request, final String username) {
    Preconditions.checkArgument(username != null && !username.isEmpty());
    final int moderatorId = apiKeyValidationService.lookupModeratorIdByApiKey(request);
    return Response.status(
        bannedNamesService.removeUsernameBan(moderatorId, username)
            ? 200
            : 400)
        .build();
  }

  @POST
  @Path(ToolboxUsernameBanClient.ADD_BANNED_USER_NAME_PATH)
  public Response addBannedUsername(@Context final HttpServletRequest request, final String username) {
    Preconditions.checkArgument(username != null && !username.isEmpty());
    final int moderatorId = apiKeyValidationService.lookupModeratorIdByApiKey(request);
    return Response.status(
        bannedNamesService.addBannedUserName(moderatorId, username)
            ? 200
            : 400)
        .build();
  }

  @GET
  @Path(ToolboxUsernameBanClient.GET_BANNED_USER_NAMES_PATH)
  public Response getBannedUsernames(@Context final HttpServletRequest request) {
    apiKeyValidationService.verifyApiKey(request);
    return Response.status(200).entity(bannedNamesService.getBannedUserNames()).build();
  }
}
