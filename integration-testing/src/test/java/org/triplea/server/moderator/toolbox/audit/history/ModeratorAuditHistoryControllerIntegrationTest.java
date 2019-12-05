package org.triplea.server.moderator.toolbox.audit.history;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxEventLogClient;
import org.triplea.server.http.AllowedUserRole;
import org.triplea.server.http.ProtectedEndpointTest;

class ModeratorAuditHistoryControllerIntegrationTest
    extends ProtectedEndpointTest<ToolboxEventLogClient> {

  private static final PagingParams PAGING_PARAMS =
      PagingParams.builder().pageSize(1).rowNumber(0).build();

  ModeratorAuditHistoryControllerIntegrationTest() {
    super(AllowedUserRole.MODERATOR, ToolboxEventLogClient::newClient);
  }

  @Test
  void fetchHistory() {
    verifyEndpointReturningCollection(client -> client.lookupModeratorEvents(PAGING_PARAMS));
  }
}
