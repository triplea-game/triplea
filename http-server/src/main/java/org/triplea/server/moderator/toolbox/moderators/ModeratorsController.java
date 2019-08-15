package org.triplea.server.moderator.toolbox.moderators;

import java.util.Optional;
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
import org.triplea.http.client.moderator.toolbox.NewApiKey;
import org.triplea.http.client.moderator.toolbox.moderator.management.ToolboxModeratorManagementClient;
import org.triplea.server.moderator.toolbox.api.key.GenerateSingleUseKeyService;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

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
  @Nonnull private final GenerateSingleUseKeyService generateSingleUseKeyService;
  @Nonnull private final ApiKeyValidationService apiKeyValidationService;

  @POST
  @Path(ToolboxModeratorManagementClient.CHECK_USER_EXISTS_PATH)
  public Response checkUserExists(
      @Context final HttpServletRequest request, final String username) {
    apiKeyValidationService.verifyApiKey(request);
    return Response.ok().entity(moderatorsService.userExistsByName(username)).build();
  }

  @GET
  @Path(ToolboxModeratorManagementClient.FETCH_MODERATORS_PATH)
  public Response getModerators(@Context final HttpServletRequest request) {
    apiKeyValidationService.verifyApiKey(request);
    return Response.ok().entity(moderatorsService.fetchModerators()).build();
  }

  @GET
  @Path(ToolboxModeratorManagementClient.IS_SUPER_MOD_PATH)
  public Response isSuperMod(@Context final HttpServletRequest request) {
    apiKeyValidationService.verifyApiKey(request);
    final Optional<Integer> result = apiKeyValidationService.lookupSuperModByApiKey(request);

    return Response.ok().entity(result.isPresent()).build();
  }

  @POST
  @Path(ToolboxModeratorManagementClient.SUPER_MOD_GENERATE_SINGLE_USE_KEY_PATH)
  public Response generateSingleUseKey(
      @Context final HttpServletRequest request, final String moderatorName) {
    apiKeyValidationService.verifySuperMod(request);
    final String newKey = generateSingleUseKeyService.generateSingleUseKey(moderatorName);

    return Response.ok().entity(new NewApiKey(newKey)).build();
  }

  @POST
  @Path(ToolboxModeratorManagementClient.REMOVE_MOD_PATH)
  public Response removeMod(@Context final HttpServletRequest request, final String moderatorName) {

    final int superModId = apiKeyValidationService.verifySuperMod(request);
    moderatorsService.removeMod(superModId, moderatorName);
    return Response.ok().build();
  }

  @POST
  @Path(ToolboxModeratorManagementClient.ADD_SUPER_MOD_PATH)
  public Response setSuperMod(
      @Context final HttpServletRequest request, final String moderatorName) {
    final int superModId = apiKeyValidationService.verifySuperMod(request);
    moderatorsService.addSuperMod(superModId, moderatorName);
    return Response.ok().build();
  }

  @POST
  @Path(ToolboxModeratorManagementClient.ADD_MODERATOR_PATH)
  public Response addModerator(@Context final HttpServletRequest request, final String username) {
    final int superModId = apiKeyValidationService.verifySuperMod(request);
    moderatorsService.addModerator(superModId, username);
    return Response.ok().build();
  }
}
