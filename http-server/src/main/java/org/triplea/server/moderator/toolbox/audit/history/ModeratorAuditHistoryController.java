package org.triplea.server.moderator.toolbox.audit.history;

import com.google.common.base.Preconditions;
import java.util.List;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.event.log.ModeratorEvent;
import org.triplea.http.client.moderator.toolbox.event.log.ToolboxEventLogClient;

/** Http server endpoints for accessing and returning moderator audit history rows. */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Builder
public class ModeratorAuditHistoryController {
  @Nonnull private final ModeratorAuditHistoryService moderatorAuditHistoryService;

  /**
   * Use this method to retrieve moderator audit history rows. Presents a paged interface.
   *
   * @param request Contains headers, used to grab and verify moderator API key which must be
   *     present.
   * @param pagingParams Parameter JSON object for page number and page size.
   */
  @POST
  @Path(ToolboxEventLogClient.AUDIT_HISTORY_PATH)
  public Response lookupAuditHistory(
      @Context final HttpServletRequest request, final PagingParams pagingParams) {
    Preconditions.checkArgument(pagingParams != null);
    Preconditions.checkArgument(pagingParams.getRowNumber() >= 0);
    Preconditions.checkArgument(pagingParams.getPageSize() > 0);

    final List<ModeratorEvent> moderatorEvents =
        moderatorAuditHistoryService.lookupHistory(
            pagingParams.getRowNumber(), pagingParams.getPageSize());

    return Response.status(200).entity(moderatorEvents).build();
  }
}
