package org.triplea.spitfire.server.controllers;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxAccessLogClient;
import org.triplea.spitfire.server.AllowedUserRole;
import org.triplea.spitfire.server.ProtectedEndpointTest;
import org.triplea.spitfire.server.SpitfireServerTestExtension;

@SuppressWarnings("UnmatchedTest")
@Disabled
@DataSet(
    value = SpitfireServerTestExtension.LOBBY_USER_DATASET + ", integration/access_log.yml",
    useSequenceFiltering = false)
class AccessLogControllerIntegrationTest extends ProtectedEndpointTest<ToolboxAccessLogClient> {

  private static final PagingParams PAGING_PARAMS =
      PagingParams.builder().pageSize(1).rowNumber(0).build();

  AccessLogControllerIntegrationTest(final URI localhost) {
    super(localhost, AllowedUserRole.MODERATOR, ToolboxAccessLogClient::newClient);
  }

  @Test
  void getAccessLog() {
    verifyEndpointReturningCollection(client -> client.getAccessLog(PAGING_PARAMS));
  }
}
