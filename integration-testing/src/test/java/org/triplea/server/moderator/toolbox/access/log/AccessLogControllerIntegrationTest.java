package org.triplea.server.moderator.toolbox.access.log;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxAccessLogClient;
import org.triplea.server.http.AuthenticatedEndpointTest;

class AccessLogControllerIntegrationTest extends AuthenticatedEndpointTest<ToolboxAccessLogClient> {

  private static final PagingParams PAGING_PARAMS =
      PagingParams.builder().pageSize(1).rowNumber(0).build();

  AccessLogControllerIntegrationTest() {
    super(ToolboxAccessLogClient::newClient);
  }

  @Test
  void getAccessLog() {
    verifyEndpointReturningCollection(client -> client.getAccessLog(PAGING_PARAMS));
  }
}
