package org.triplea.server.moderator.toolbox.bad.words;

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

import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeySecurityService;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

import lombok.Builder;

/**
 * Controller for servicing moderator toolbox bad-words tab (provides CRUD operations).
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Builder
public class BadWordsController {
  @Nonnull
  private final BadWordsService badWordsService;
  @Nonnull
  private final ApiKeySecurityService apiKeySecurityService;
  @Nonnull
  private final ApiKeyValidationService apiKeyValidationService;

  @POST
  @Path(ModeratorToolboxClient.BAD_WORD_REMOVE_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response removeBadWord(@Context final HttpServletRequest request, final String word) {
    if (!apiKeySecurityService.allowValidation(request)) {
      return ApiKeyValidationService.LOCK_OUT_RESPONSE;
    }
    return apiKeyValidationService.lookupModeratorIdByApiKey(request)
        .map(moderatorId -> badWordsService.removeBadWord(moderatorId, word)
            ? Response
                .status(200)
                .entity(ModeratorToolboxClient.SUCCESS)
                .build()
            : Response
                .status(400)
                .entity(word + " was not removed, it may already have been deleted")
                .build())
        .orElse(ApiKeyValidationService.API_KEY_NOT_FOUND_RESPONSE);
  }


  @POST
  @Path(ModeratorToolboxClient.BAD_WORD_ADD_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addBadWord(@Context final HttpServletRequest request, final String word) {
    if (!apiKeySecurityService.allowValidation(request)) {
      return ApiKeyValidationService.LOCK_OUT_RESPONSE;
    }

    return apiKeyValidationService.lookupModeratorIdByApiKey(request)
        .map(moderatorId -> badWordsService.addBadWord(moderatorId, word)
            ? Response
                .status(200)
                .entity(ModeratorToolboxClient.SUCCESS)
                .build()
            : Response
                .status(400)
                .entity(word + " was not added, it may already have been added")
                .build())
        .orElse(ApiKeyValidationService.API_KEY_NOT_FOUND_RESPONSE);
  }

  @GET
  @Path(ModeratorToolboxClient.BAD_WORD_GET_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getBadWords(@Context final HttpServletRequest request) {
    if (!apiKeySecurityService.allowValidation(request)) {
      return ApiKeyValidationService.LOCK_OUT_RESPONSE;
    }

    if (!apiKeyValidationService.lookupModeratorIdByApiKey(request).isPresent()) {
      return ApiKeyValidationService.API_KEY_NOT_FOUND_RESPONSE;
    }

    return Response.status(200).entity(badWordsService.getBadWords()).build();
  }
}
