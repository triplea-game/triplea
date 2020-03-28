package org.triplea.modules.moderation.access.log;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.data.UserRole;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxAccessLogClient;

/** Controller to query the access log table, for us by moderators. */
@Builder
@RolesAllowed(UserRole.MODERATOR)
public class AccessLogController extends HttpController {
  @Nonnull private final AccessLogService accessLogService;

  public static AccessLogController build(final Jdbi jdbi) {
    return AccessLogController.builder() //
        .accessLogService(AccessLogService.build(jdbi))
        .build();
  }

  @POST
  @Path(ToolboxAccessLogClient.FETCH_ACCESS_LOG_PATH)
  public Response fetchAccessLog(final PagingParams pagingParams) {
    return Response.ok().entity(accessLogService.fetchAccessLog(pagingParams)).build();
  }
}
