package org.triplea.modules.moderation.audit.history;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxEventLogClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.LobbyServerTest;
import org.triplea.modules.http.ProtectedEndpointTest;

@DataSet(
    value = LobbyServerTest.LOBBY_USER_DATASET + ", integration/moderator_action_history.yml",
    useSequenceFiltering = false)
class ModeratorAuditHistoryControllerIntegrationTest
    extends ProtectedEndpointTest<ToolboxEventLogClient> {

  private static final PagingParams PAGING_PARAMS =
      PagingParams.builder().pageSize(1).rowNumber(0).build();

  ModeratorAuditHistoryControllerIntegrationTest(final URI localhost) {
    super(localhost, AllowedUserRole.MODERATOR, ToolboxEventLogClient::newClient);
  }

  @Test
  void fetchHistory() {
    verifyEndpointReturningCollection(client -> client.lookupModeratorEvents(PAGING_PARAMS));
  }
}
