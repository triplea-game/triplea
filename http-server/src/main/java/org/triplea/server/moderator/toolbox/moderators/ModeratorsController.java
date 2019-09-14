package org.triplea.server.moderator.toolbox.moderators;

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
import lombok.Builder;
import org.triplea.http.client.moderator.toolbox.moderator.management.ToolboxModeratorManagementClient;

/**
 * Provides endpoint for moderator maintenance actions and to support the moderators toolbox
 * 'moderators' tab. Actions include: adding moderators, removing moderators, and promoting
 * moderators to 'super-mod'. Some actions are only allowed for super-mods.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Builder
public class ModeratorsController {
  @Nonnull private final ModeratorsService moderatorsService;

  @POST
  @Path(ToolboxModeratorManagementClient.CHECK_USER_EXISTS_PATH)
  public Response checkUserExists(
      @Context final HttpServletRequest request, final String username) {
    return Response.ok().entity(moderatorsService.userExistsByName(username)).build();
  }

  @GET
  @Path(ToolboxModeratorManagementClient.FETCH_MODERATORS_PATH)
  public Response getModerators(@Context final HttpServletRequest request) {
    return Response.ok().entity(moderatorsService.fetchModerators()).build();
  }

  @GET
  @Path(ToolboxModeratorManagementClient.IS_SUPER_MOD_PATH)
  public Response isSuperMod(@Context final HttpServletRequest request) {
    // TODO: Project#12 re-implement this
    return Response.ok().entity(false).build();
  }

  @POST
  @Path(ToolboxModeratorManagementClient.REMOVE_MOD_PATH)
  public Response removeMod(@Context final HttpServletRequest request, final String moderatorName) {
    // TODO: Project#12 grab moderator id from auth parameter
    final int superModId = 0;
    moderatorsService.removeMod(superModId, moderatorName);
    return Response.ok().build();
  }

  @POST
  @Path(ToolboxModeratorManagementClient.ADD_SUPER_MOD_PATH)
  public Response setSuperMod(
      @Context final HttpServletRequest request, final String moderatorName) {
    // TODO: Project#12 grab moderator id from auth parameter
    final int superModId = 0;
    moderatorsService.addSuperMod(superModId, moderatorName);
    return Response.ok().build();
  }

  @POST
  @Path(ToolboxModeratorManagementClient.ADD_MODERATOR_PATH)
  public Response addModerator(@Context final HttpServletRequest request, final String username) {
    // TODO: Project#12 grab moderator id from auth parameter
    final int superModId = 0;
    moderatorsService.addModerator(superModId, username);
    return Response.ok().build();
  }
}
