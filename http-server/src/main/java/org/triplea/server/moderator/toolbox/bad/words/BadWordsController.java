package org.triplea.server.moderator.toolbox.bad.words;

import com.google.common.base.Preconditions;
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
import org.triplea.http.client.moderator.toolbox.bad.words.ToolboxBadWordsClient;

/** Controller for servicing moderator toolbox bad-words tab (provides CRUD operations). */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Builder
public class BadWordsController {
  @Nonnull private final BadWordsService badWordsService;

  /**
   * Removes a bad word entry from the bad-word table.
   *
   * @param request Used to get the moderator-api-key header and validate it.
   * @param word The new bad word entry to remove. We expect this to exist in the table or else we
   *     return a 400.
   */
  @POST
  @Path(ToolboxBadWordsClient.BAD_WORD_REMOVE_PATH)
  public Response removeBadWord(@Context final HttpServletRequest request, final String word) {
    Preconditions.checkArgument(word != null && !word.isEmpty());
    // TODO: Project#12 get moderator ID from authorized user @Auth param
    final int moderatorId = 0;
    return badWordsService.removeBadWord(moderatorId, word)
        ? Response.ok().build()
        : Response.status(400)
            .entity(word + " was not removed, it may already have been deleted")
            .build();
  }

  /**
   * Adds a bad word entry to the bad-word table.
   *
   * @param request Used to get the moderator-api-key header and validate it.
   * @param word The new bad word entry to add.
   */
  @POST
  @Path(ToolboxBadWordsClient.BAD_WORD_ADD_PATH)
  public Response addBadWord(@Context final HttpServletRequest request, final String word) {
    Preconditions.checkArgument(word != null && !word.isEmpty());
    // TODO: Project#12 get moderator ID from authorized user @Auth param
    final int moderatorId = 0;
    return badWordsService.addBadWord(moderatorId, word)
        ? Response.ok().build()
        : Response.status(400)
            .entity(word + " was not added, it may already have been added")
            .build();
  }

  @GET
  @Path(ToolboxBadWordsClient.BAD_WORD_GET_PATH)
  public Response getBadWords(@Context final HttpServletRequest request) {
    return Response.status(200).entity(badWordsService.getBadWords()).build();
  }
}
