package org.triplea.server.moderator.toolbox.access.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.access.log.AccessLogData;
import org.triplea.http.client.moderator.toolbox.access.log.ToolboxAccessLogClient;
import org.triplea.server.http.AbstractDropwizardTest;

class AccessLogControllerIntegrationTest extends AbstractDropwizardTest {

  private static final PagingParams PAGING_PARAMS = PagingParams.builder()
      .pageSize(1)
      .rowNumber(0)
      .build();

  @Test
  void getAccessLog() {
    final ToolboxAccessLogClient client =
        AbstractDropwizardTest.newClient(ToolboxAccessLogClient::newClient);
    final List<AccessLogData> results = client.getAccessLog(PAGING_PARAMS);

    assertThat(results, not(empty()));
  }

  @Test
  void getAccessLogWithoutAuthorization() {
    final ToolboxAccessLogClient client =
        AbstractDropwizardTest.newClientWithInvalidCreds(ToolboxAccessLogClient::newClient);

    assertThrows(HttpInteractionException.class, () -> client.getAccessLog(PAGING_PARAMS));
  }
}
