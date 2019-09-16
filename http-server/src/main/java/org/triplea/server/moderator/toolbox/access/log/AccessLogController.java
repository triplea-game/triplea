package org.triplea.server.moderator.toolbox.access.log;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.access.log.ToolboxAccessLogClient;
import org.triplea.lobby.server.db.data.UserRole;

/** Controller to query the access log table, for us by moderators. */
@Builder
@Path("")
@RolesAllowed(UserRole.MODERATOR)
public class AccessLogController {

  @Nonnull private final AccessLogService accessLogService;

  @POST
  @Path(ToolboxAccessLogClient.FETCH_ACCESS_LOG_PATH)
  public Response fetchAccessLog(final PagingParams pagingParams) {
    return Response.ok().entity(accessLogService.fetchAccessLog(pagingParams)).build();
  }
}
