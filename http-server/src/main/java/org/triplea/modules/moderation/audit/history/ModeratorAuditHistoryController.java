package org.triplea.modules.moderation.audit.history;

import com.google.common.base.Preconditions;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.triplea.db.data.UserRole;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.ModeratorEvent;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxEventLogClient;

/** Http server endpoints for accessing and returning moderator audit history rows. */
@Builder
@RolesAllowed(UserRole.MODERATOR)
public class ModeratorAuditHistoryController extends HttpController {
  @Nonnull private final ModeratorAuditHistoryService moderatorAuditHistoryService;

  /**
   * Use this method to retrieve moderator audit history rows. Presents a paged interface.
   *
   * @param pagingParams Parameter JSON object for page number and page size.
   */
  @POST
  @Path(ToolboxEventLogClient.AUDIT_HISTORY_PATH)
  public Response lookupAuditHistory(final PagingParams pagingParams) {
    Preconditions.checkArgument(pagingParams != null);
    Preconditions.checkArgument(pagingParams.getRowNumber() >= 0);
    Preconditions.checkArgument(pagingParams.getPageSize() > 0);

    final List<ModeratorEvent> moderatorEvents =
        moderatorAuditHistoryService.lookupHistory(
            pagingParams.getRowNumber(), pagingParams.getPageSize());

    return Response.status(200).entity(moderatorEvents).build();
  }
}
