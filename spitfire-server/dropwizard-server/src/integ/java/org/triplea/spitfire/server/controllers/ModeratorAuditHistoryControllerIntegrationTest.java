package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.ModeratorEvent;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxEventLogClient;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class ModeratorAuditHistoryControllerIntegrationTest extends ControllerIntegrationTest {
  private final URI localhost;
  private final ToolboxEventLogClient client;

  ModeratorAuditHistoryControllerIntegrationTest(final URI localhost) {
    this.localhost = localhost;
    this.client = ToolboxEventLogClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
  }

  @SuppressWarnings("unchecked")
  @Test
  void mustBeAuthorized() {
    assertNotAuthorized(
        ControllerIntegrationTest.NOT_MODERATORS,
        apiKey -> ToolboxEventLogClient.newClient(localhost, apiKey),
        client ->
            client.lookupModeratorEvents(PagingParams.builder().pageSize(1).rowNumber(0).build()));
  }

  @Test
  void fetchHistory() {
    final List<ModeratorEvent> response =
        client.lookupModeratorEvents(PagingParams.builder().pageSize(1).rowNumber(0).build());
    assertThat(response, not(empty()));
    assertThat(response.get(0).getActionTarget(), notNullValue());
    assertThat(response.get(0).getModeratorAction(), notNullValue());
    assertThat(response.get(0).getDate(), notNullValue());
    assertThat(response.get(0).getModeratorName(), notNullValue());
  }
}
