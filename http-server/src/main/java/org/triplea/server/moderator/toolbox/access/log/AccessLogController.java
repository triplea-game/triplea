package org.triplea.server.moderator.toolbox.access.log;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.access.log.ToolboxAccessLogClient;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

/** Controller to query the access log table, for us by moderators. */
@Builder
@Path("")
public class AccessLogController {

  @Nonnull private final ApiKeyValidationService apiKeyValidationService;
  @Nonnull private final AccessLogService accessLogService;

  @POST
  @Path(ToolboxAccessLogClient.FETCH_ACCESS_LOG_PATH)
  public Response fetchAccessLog(
      @Context final HttpServletRequest request, final PagingParams pagingParams) {
    apiKeyValidationService.verifyApiKey(request);
    return Response.ok().entity(accessLogService.fetchAccessLog(pagingParams)).build();
  }
}
