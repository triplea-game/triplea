package org.triplea.server.moderator.toolbox.banned.names;

import com.google.common.base.Preconditions;
import io.dropwizard.auth.Auth;
import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.triplea.http.client.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.lobby.server.db.data.UserRole;
import org.triplea.server.access.AuthenticatedUser;

/** Endpoint for use by moderators to view, add and remove player username bans. */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Builder
@RolesAllowed(UserRole.MODERATOR)
public class UsernameBanController {
  @Nonnull private final UsernameBanService bannedNamesService;

  @POST
  @Path(ToolboxUsernameBanClient.REMOVE_BANNED_USER_NAME_PATH)
  public Response removeBannedUsername(
      @Auth final AuthenticatedUser authenticatedUser, final String username) {
    Preconditions.checkArgument(username != null && !username.isEmpty());
    return Response.status(
            bannedNamesService.removeUsernameBan(authenticatedUser.getUserId(), username)
                ? 200
                : 400)
        .build();
  }

  @POST
  @Path(ToolboxUsernameBanClient.ADD_BANNED_USER_NAME_PATH)
  public Response addBannedUsername(
      @Auth final AuthenticatedUser authenticatedUser, final String username) {
    Preconditions.checkArgument(username != null && !username.isEmpty());
    return Response.status(
            bannedNamesService.addBannedUserName(authenticatedUser.getUserId(), username)
                ? 200
                : 400)
        .build();
  }

  @GET
  @Path(ToolboxUsernameBanClient.GET_BANNED_USER_NAMES_PATH)
  public Response getBannedUsernames() {
    return Response.status(200).entity(bannedNamesService.getBannedUserNames()).build();
  }
}
