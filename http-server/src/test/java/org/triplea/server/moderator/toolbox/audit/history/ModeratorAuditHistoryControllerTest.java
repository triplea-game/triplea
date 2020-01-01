package org.triplea.server.moderator.toolbox.audit.history;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.ModeratorEvent;

@ExtendWith(MockitoExtension.class)
class ModeratorAuditHistoryControllerTest {

  private static final ModeratorEvent EVENT_1 =
      ModeratorEvent.builder()
          .date(Instant.now())
          .actionTarget("Desolation is a salty corsair.")
          .moderatorAction("Jolly, small grace!")
          .moderatorName("Yardarms are the pants of the golden greed.")
          .build();

  private static final int ROW_NUMBER = 60;
  private static final int ROW_COUNT = 50;

  @Mock private ModeratorAuditHistoryService moderatorAuditHistoryService;

  @InjectMocks private ModeratorAuditHistoryController moderatorAuditHistoryController;

  @Test
  void verifiesPagingParameters() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            moderatorAuditHistoryController.lookupAuditHistory(
                PagingParams.builder().rowNumber(-1).pageSize(100).build()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            moderatorAuditHistoryController.lookupAuditHistory(
                PagingParams.builder().rowNumber(0).pageSize(0).build()));
  }

  @Test
  void lookupAuditHistory() {
    when(moderatorAuditHistoryService.lookupHistory(ROW_NUMBER, ROW_COUNT))
        .thenReturn(ImmutableList.of(EVENT_1));

    final Response response =
        moderatorAuditHistoryController.lookupAuditHistory(
            PagingParams.builder().rowNumber(ROW_NUMBER).pageSize(ROW_COUNT).build());

    assertThat(response.getStatus(), is(200));
    assertThat(((List<?>) response.getEntity()).get(0), is(EVENT_1));
  }
}
