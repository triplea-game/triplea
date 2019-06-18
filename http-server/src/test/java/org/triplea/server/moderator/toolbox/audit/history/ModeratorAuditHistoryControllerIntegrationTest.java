package org.triplea.server.moderator.toolbox.audit.history;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.event.log.ToolboxEventLogClient;
import org.triplea.server.http.AbstractDropwizardTest;

class ModeratorAuditHistoryControllerIntegrationTest extends AbstractDropwizardTest {
  private static final PagingParams PAGING_PARAMS = PagingParams.builder()
      .pageSize(1)
      .rowNumber(0)
      .build();

  @Test
  void fetchHistory() {
    assertThat(
        AbstractDropwizardTest.newClient(ToolboxEventLogClient::newClient)
            .lookupModeratorEvents(PAGING_PARAMS),
        not(empty()));
  }

  @Test
  void fetchHistoryNotAuthorized() {
    assertThrows(
        HttpInteractionException.class,
        () -> AbstractDropwizardTest.newClientWithInvalidCreds(ToolboxEventLogClient::newClient)
            .lookupModeratorEvents(PAGING_PARAMS));
  }
}
