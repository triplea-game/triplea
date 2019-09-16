package org.triplea.server.moderator.toolbox.audit.history;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.event.log.ToolboxEventLogClient;
import org.triplea.server.http.ProtectedEndpointTest;

class ModeratorAuditHistoryControllerIntegrationTest
    extends ProtectedEndpointTest<ToolboxEventLogClient> {

  private static final PagingParams PAGING_PARAMS =
      PagingParams.builder().pageSize(1).rowNumber(0).build();

  ModeratorAuditHistoryControllerIntegrationTest() {
    super(ToolboxEventLogClient::newClient);
  }

  @Test
  void fetchHistory() {
    verifyEndpointReturningCollection(client -> client.lookupModeratorEvents(PAGING_PARAMS));
  }
}
