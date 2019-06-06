package org.triplea.server.moderator.toolbox.audit.history;

import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.triplea.http.client.moderator.toolbox.ModeratorEvent;
import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeySecurityService;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

import com.google.common.base.Preconditions;

import lombok.Builder;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Builder
// TODO: test-me
public class ModeratorAuditHistoryController {
  @Nonnull
  private final ApiKeySecurityService apiKeySecurityService;
  @Nonnull
  private final ApiKeyValidationService apiKeyValidationService;
  @Nonnull
  private final ModeratorAuditHistoryService moderatorAuditHistoryService;


  @GET
  @Path(ModeratorToolboxClient.AUDIT_HISTORY_PATH)
  public Response lookupAuditHistory(
      @Context final HttpServletRequest request,
      @QueryParam(ModeratorToolboxClient.ROW_START_PARAM) final Integer rowNumber,
      @QueryParam(ModeratorToolboxClient.ROW_COUNT_PARAM) final Integer rowCount) {
    Preconditions.checkArgument(rowNumber != null);
    Preconditions.checkArgument(rowCount != null);

    if (!apiKeySecurityService.allowValidation(request)) {
      return ApiKeyValidationService.LOCK_OUT_RESPONSE;
    }
    if (!apiKeyValidationService.lookupModeratorIdByApiKey(request).isPresent()) {
      return ApiKeyValidationService.API_KEY_NOT_FOUND_RESPONSE;
    }

    final List<ModeratorEvent> moderatorEvents =
        moderatorAuditHistoryService.lookupHistory(rowNumber, rowCount);

    return Response
        .status(200)
        .entity(moderatorEvents)
        .build();
  }
}
